package org.legalscheduler;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.DefaultCaret;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.legalscheduler.domain.Schedule;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.constraint.ConstraintMatch;
import org.optaplanner.core.api.score.constraint.ConstraintMatchTotal;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.impl.score.director.ScoreDirector;
import org.optaplanner.core.impl.score.director.ScoreDirectorFactory;

public class SchedulerApplication extends JFrame implements ActionListener {
    
    private static final long serialVersionUID = 1L;

    public static SimpleDateFormat INPUT_DATE_FORMAT = new SimpleDateFormat("M/d/yyyy"); 
    public static SimpleDateFormat OUTPUT_DATE_FORMAT = new SimpleDateFormat("M/d/yyyy");
    
    JButton selectFileButton;
    JLabel selectedFileLabel;
    JFileChooser fc;
    JPanel statusPanel;
    JPanel resultsPanel;
    
    Icon processingIcon;
    Icon warningIcon;
    Icon errorIcon;
    Icon okIcon;
    
    // These are useful for development, but less useful for end users.
    boolean showScore = false;
    boolean showStackTrace = false;
    
    public static void main(String[] args) {
        try {
            System.setProperty("J2D_D3D", "false");
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // If Nimbus is not available, you can set the GUI to another look and feel.
        }
        
        SchedulerApplication app = new SchedulerApplication();
        app.pack();
        app.setSize(600, 450);
        app.setVisible(true);
       
    }
    
    
    
    public SchedulerApplication() {
        super("Legal Aid Attorney Scheduler");
        setContentPane(createContentPane());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == selectFileButton) {
            int returnVal = fc.showOpenDialog(this);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                selectedFileLabel.setText("Processing " + file.getAbsolutePath());
                statusPanel.removeAll();
                statusPanel.add(createStatusLabel("Processing...", processingIcon));
                resultsPanel.removeAll();
                
                selectFileButton.setEnabled(false);
                // This call seems to be necessary in order for the UI to
                // refresh correctly if the same CSV is selected twice in a row.
                paintAll(getGraphics());
                
                File outputFile = getOutputFile(file);
                SolveWorker worker = new SolveWorker(file, outputFile);
                worker.execute();            
            } else {
                selectedFileLabel.setText("None selected");
                statusPanel.removeAll();
                resultsPanel.removeAll();
            }
        }
        
    }
    
    private Solver solve(File input, File output) {
        SolverFactory solverFactory = SolverFactory.createFromXmlResource("schedulerSolverConfig.xml");

        // TODO: Consider making the time spent user-configurable.
        //SolverConfig solverConfig = solverFactory.getSolverConfig();
        //TerminationConfig terminationConfig = solverConfig.getTerminationConfig();
        //terminationConfig.setMinutesSpentLimit(userInput);

        Solver solver = solverFactory.buildSolver();
        ScheduleLoader loader = new ScheduleLoader();
        Schedule schedule = loader.loadSchedule(input);
        
        solver.solve(schedule);
        
        ScheduleExporter exporter = new ScheduleExporter();
        Schedule bestSolution = (Schedule)solver.getBestSolution();
        exporter.export(bestSolution, output);
        return solver;
    }
    
    private void handleSolution(Solver solver, File outputFile) {
        
        HardSoftScore score = (HardSoftScore)solver.getBestSolution().getScore();
        statusPanel.removeAll();
        boolean isValid = score.getHardScore() == 0;
        if (isValid) {
            statusPanel.add(createStatusLabel("A valid schedule was created", okIcon));
        } else {
            statusPanel.add(createStatusLabel("A valid schedule could not be created", warningIcon));
        }
        
        resultsPanel.removeAll();
        resultsPanel.add(createLabel("Your schedule has been written to: "));
        resultsPanel.add(createVerticalFiller());
        resultsPanel.add(createLabel("  " + outputFile.getAbsolutePath()));
        resultsPanel.add(createVerticalFiller());
        resultsPanel.add(createFileLink(outputFile));
        resultsPanel.add(createVerticalFiller());
        
        if (score.getHardScore() < 0 || score.getSoftScore() < 0) {
            String label = (isValid) ? "Unfortunately, it's not perfect. Some preferences couldn't be met." 
                            : "Here are the problems.";
            if (showScore) {
                label += " Hard Score: " + score.getHardScore() + ", Soft Score: " + score.getSoftScore();
            }
                
            ScoreDirectorFactory scoreDirectorFactory = solver.getScoreDirectorFactory();
            ScoreDirector scoreDirector = scoreDirectorFactory.buildScoreDirector();
            scoreDirector.setWorkingSolution(solver.getBestSolution());
            scoreDirector.calculateScore();
            StringBuilder problems = new StringBuilder();
            for (ConstraintMatchTotal constraintMatchTotal : scoreDirector.getConstraintMatchTotals()) {
                int numViolations = constraintMatchTotal.getConstraintMatchCount();
                if (numViolations > 0) {       
                    String violationStr = (numViolations == 1) ? " violation" : " violations";
                    problems.append(constraintMatchTotal.getConstraintName() + " - " + numViolations + violationStr + System.lineSeparator());
                    for (ConstraintMatch match : constraintMatchTotal.getConstraintMatchSet()) {
                        String justification = match.getJustificationList().toString();
                        justification = StringUtils.strip(justification, "[]");
                        problems.append(justification + System.lineSeparator());
                    }
                    
                }
            }
            resultsPanel.add(createProblemsPanel(label, problems.toString()));
        }
        
    }
    
    private Container createContentPane() {
        JPanel contentPane = new JPanel(new BorderLayout(5, 5));
        contentPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JLabel titleLabel = new JLabel("Legal Aid Attorney Scheduler", JLabel.CENTER);
        titleLabel.setFont(titleLabel.getFont().deriveFont(16.0f));
        contentPane.add(titleLabel, BorderLayout.NORTH);
        contentPane.add(createMainPanel(), BorderLayout.CENTER);
        return contentPane;
    }
    
    private Container createMainPanel() {
        JPanel panel = new JPanel();
        panel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        selectFileButton = new JButton("Select an availability spreadsheet (csv format)");
        selectFileButton.addActionListener(this);
        panel.add(createVerticalFiller());
        panel.add(selectFileButton);
        panel.add(createVerticalFiller());
        selectedFileLabel = createLabel("None selected");
        panel.add(selectedFileLabel);
        panel.add(createVerticalFiller());
        panel.add(createStatusPanel());
        panel.add(createVerticalFiller());
        panel.add(createResultsPanel());
        
        fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setFileFilter(new CsvFilter());
        
        processingIcon = new ImageIcon(getClass().getResource("/org/legalscheduler/images/processing.gif"));
        warningIcon = new ImageIcon(getClass().getResource("/org/legalscheduler/images/warning.png"));
        errorIcon = new ImageIcon(getClass().getResource("/org/legalscheduler/images/error.png"));
        okIcon = new ImageIcon(getClass().getResource("/org/legalscheduler/images/ok.png"));
        return panel;
    }
    
    private Container createStatusPanel() {
        statusPanel = new JPanel();
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.LINE_AXIS));
        statusPanel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        return statusPanel;
    }
    
    private Container createResultsPanel() {
        resultsPanel = new JPanel();
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.PAGE_AXIS));
        resultsPanel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        return resultsPanel;
    }
    
    private JPanel createProblemsPanel(String label, String problems) {
        JPanel problemsPanel = new JPanel(new BorderLayout(2, 2));
        problemsPanel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        problemsPanel.add(new JLabel(label), BorderLayout.NORTH);
        JTextArea problemsTextArea = new JTextArea(8, 70);
        problemsTextArea.setAlignmentX(JTextArea.LEFT_ALIGNMENT);
        problemsTextArea.setEditable(false);
        DefaultCaret caret = (DefaultCaret)problemsTextArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        problemsTextArea.setText(problems);
        JScrollPane scrollPane = new JScrollPane(problemsTextArea,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        problemsPanel.add(scrollPane, BorderLayout.CENTER);
        scrollPane.getVerticalScrollBar().setValue(0);
        scrollPane.getHorizontalScrollBar().setValue(0);
        scrollPane.setAlignmentX(JScrollPane.LEFT_ALIGNMENT);
        return problemsPanel;
    }
    
    private String formatErrorMessage(Exception e) {
        String msg = e.getMessage();
        if (showStackTrace) {
            msg += System.lineSeparator() 
                    + "---- Stack Trace ----" + System.lineSeparator() 
                    + ExceptionUtils.getStackTrace(e);
        }
        return msg;
    }
    
    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        return label;
    }
    
    private JButton createButton(String text) {
        JButton button = new JButton(text);
        button.setAlignmentX(JButton.LEFT_ALIGNMENT);
        return button;
    }
   
    
    
    
   
    
    private Box.Filler createVerticalFiller() {
        Dimension minSize = new Dimension(5, 5);
        Dimension prefSize = new Dimension(5, 5);
        Dimension maxSize = new Dimension(5, 10);
        Box.Filler filler = new Box.Filler(minSize, prefSize, maxSize);
        filler.setAlignmentX(LEFT_ALIGNMENT);
        return filler;
    }
    
    private JButton createFileLink(File file) {
        JButton button = createButton("Click to View");
        button.addActionListener(new OpenUrlAction(file));
        return button;
    }
    
    private JLabel createStatusLabel(String text, Icon icon) {
        JLabel processingLabel = createLabel(text);
        processingLabel.setIcon(icon);
        processingLabel.setVerticalTextPosition(JLabel.CENTER);
        processingLabel.setHorizontalTextPosition(JLabel.RIGHT);
        processingLabel.setVerticalAlignment(JLabel.CENTER);
        return processingLabel;
    }
    
    private File getOutputFile(File inputFile) {
        File parentDir = inputFile.getParentFile();
        DateFormat df = new SimpleDateFormat("MMM dd yyyy");
        String preferredName = "Schedule - " + df.format(new Date());
        File outputFile = new File(parentDir, preferredName + ".csv");
        if (outputFile.exists()) {
            int index = 1;
            while (outputFile.exists()) {
                outputFile = new File(parentDir, preferredName + " (" + index + ").csv");
                ++ index;
            }
        }
        return outputFile;
                
    }
    
    protected class SolveWorker extends SwingWorker<Solver, Void> {

        private File input;
        private File output;

        public SolveWorker(File input, File output) {
            this.input = input;
            this.output = output;
        }

        @Override
        protected Solver doInBackground() throws Exception {
            return solve(input, output);
        }

        @Override
        protected void done() {
            try {
                Solver solver = get();
                handleSolution(solver, output);
            } catch (InterruptedException e) {
                statusPanel.removeAll();
                statusPanel.add(createStatusLabel("An interrupting error occurred", errorIcon));
                resultsPanel.removeAll();
                resultsPanel.add(createProblemsPanel("Error Message", formatErrorMessage(e)));
            } catch (ExecutionException e) {
                statusPanel.removeAll();
                statusPanel.add(createStatusLabel("An error occurred", errorIcon));
                resultsPanel.removeAll();
                resultsPanel.add(createProblemsPanel("Error Message", formatErrorMessage(e)));
            } finally {
                selectFileButton.setEnabled(true);
                validate();
            }
        }

    }
    
    private class CsvFilter extends FileFilter {
        @Override
        public boolean accept(File f) {
            if (f.isDirectory()) {
                return true;
            }
            String extension = StringUtils.substringAfterLast(f.getName(), ".");
            if (extension != null) {
                if (extension.toLowerCase().equals("csv")) {
                    return true;
                } else {
                    return false;
                }
            }

            return false;
        }
        @Override
        public String getDescription() {
            return ("*.csv");
        }

    }
    
    class OpenUrlAction implements ActionListener {
        public OpenUrlAction(File file) {
            this.uri = file.toURI();
        }
        private URI uri;
        @Override public void actionPerformed(ActionEvent e) {
            if (Desktop.isDesktopSupported()) {
                try {
                  Desktop.getDesktop().browse(uri);
                } catch (IOException error) { 
                    /* TODO: error handling */ 
                }
            }
        }
      }
    
    
    
    
}
