/**
 * 
 * @author Jean-Denis Greze [jg253@cs.columbia.edu]
 * @author Gaurav S. Kc [gskc@cs.columbia.edu]
 * 
 * 2-do:
 *  - use something other than Version.data2() to retrieve the
 *  taskName from a version object
 *
 * 2-do:
 *  - add commandLine flag "-g" to ProcessorMain.java
 *  - set global boolean variable to 'enable' GUI mode
 *  - edit Processor.n so that it will update
 *    _procPanel in the GUI, accessible thru a global
 *    variable ... have GUI poll processor state
 *  - edit Processor.executeTask(...) so that it will
 *    update _taskPanel in the GUI ... have GUI poll processor state
 *  - add a timer in Processor.executeTask(...) so that
 *    it will timeout if the given task does not complete
 *    execution in the specified amount of time
 * 
 * third tab: replicating
*/

package psl.survivor.demo;


import psl.survivor.ProcessorMain;

import psl.survivor.proc.Log;
import psl.survivor.proc.nrl.NRLProcessData;
import psl.survivor.proc.TaskProcessorHandle;

import psl.survivor.util.Version;
import psl.survivor.util.NameValuePair;

import java.awt.event.*;
import java.awt.*;
import javax.swing.*;

import java.awt.GridLayout;
import java.awt.BorderLayout;

import java.awt.Font;
import java.awt.Color;
import java.awt.Component;

import java.util.Map;
import java.util.Date;
import java.util.TreeMap;

import javax.swing.JList;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JSplitPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.DefaultListModel;
import javax.swing.ListCellRenderer;

/**
 * GUI-window to be used for demonstration purposes
 */
public final class NPortal {
  // constants ////////////////////////////////////////////////////////////////
  private final int _width = 500;
  private final int _height = 300;
  // end of constants /////////////////////////////////////////////////////////

  // global variables /////////////////////////////////////////////////////////
  final JFrame _frame;
  final JTabbedPane _pane;
  final JSplitPane _sPane;
  // end of global variables //////////////////////////////////////////////////
  
  /**
   * implementation of psl.survivor.proc.LOG used for graphical execution
   */
  public final Logger loggerInstance = new Logger();  

  /**
   * setup frame and main Label
   * 
   */
  public NPortal(String title) {
    _frame = new JFrame(title);
    _pane = new JTabbedPane();
    
    _frame.getContentPane().add(_pane);
    _frame.setSize(_width, _height);
    _frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    _procScrollPane = new JScrollPane(_procPanel = new JPanel());
    _taskScrollPane = new JScrollPane(_taskPanel = new JPanel());
    _replScrollPane = new JScrollPane(_replPanel = new JPanel());
    
    // add the processor-info display panel
    _pane.addTab("Processor", null, initProcPanel(), 
                 "Information on processor");
    
    // add the task-info, replicating-info display panels
    _pane.addTab("Tasks and Replications", null, 
                 _sPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, 
                                         initTaskPanel(), 
                                         initReplPanel()) {
                                         
                      int lastLoc;
                      long lastSet = new Date().getTime();
                      long currTime;
                      public void paint(Graphics g) {
                        super.paint(g);
                        currTime = new Date().getTime();
                        if (Math.abs(getDividerLocation() - lastLoc) >= 0.05) { 
                          setDividerLocation(0.5);
                          lastLoc = getDividerLocation();
                          lastSet = currTime;
                        } else if (currTime - lastSet >= 200) {
                          setDividerLocation(0.5);
                          lastSet = currTime;
                        }
                      }
                    }, 
                 "Information on executing tasks");
    
    if (ProcessorMain.debug) testing();
    _frame.show();
  }
  
  // Processor-capability CELL-RENDERER /////////////////////////////
  final static class ProcCellRenderer extends JPanel implements ListCellRenderer {
    final Font headerFont = new Font("Verdana", Font.BOLD, 18);
    final Font regularFont = new Font("Verdana", Font.PLAIN, 12);
    final JLabel _key = new JLabel();
    final JLabel _val = new JLabel();
    ProcCellRenderer() {
      setLayout(new GridLayout(1, 2, 5, 5));
      add(_key); add(_val);
    }
    public Component getListCellRendererComponent(JList list,
        Object value, int index, boolean isSelected, boolean cellHasFocus) {
      if (value.toString().equals("HEADER")) {
        _key.setText("Capability name");
        _val.setText("Capability value");
        _key.setFont(headerFont);
        _val.setFont(headerFont);
        setBackground(Color.gray);
      } else if (value instanceof NameValuePair) {
        NameValuePair nvp = (NameValuePair) value;
        _key.setText("" + nvp.getName());
        _val.setText("" + nvp.getValue());
        _key.setFont(regularFont);
        _val.setFont(regularFont);
        setBackground(Color.lightGray);
      }
      return this;
    }
  }
  
  final JPanel _procPanel;
  final JScrollPane _procScrollPane;
  final DefaultListModel _procListModel = new DefaultListModel();
  final JList _procList = new JList(_procListModel);
  
  /**
   * initialise the processor-info display panel
   * 
   */
  private JScrollPane initProcPanel() {
    _procList.setCellRenderer(new ProcCellRenderer());
    _procListModel.addElement("HEADER");
    
    _procPanel.setLayout(new BorderLayout());
    _procPanel.add(new JLabel("Processor capabilities"), BorderLayout.NORTH);
    _procPanel.add(_procList, BorderLayout.CENTER);
    
    return (_procScrollPane);
  }
  
  // CELL-RENDERER //////////////////////////////////////////////////
  final static class CellRenderer extends JPanel implements ListCellRenderer {
    final JLabel _labelTask = new JLabel();
    final JLabel _labelProc;
    final JLabel _labelState = new JLabel();
    final JLabel _labelProgress = new JLabel();
    final JPanel _panelProgress = new JPanel();
    final Font headerFont = new Font("Verdana", Font.BOLD, 18);
    final Font regularFont = new Font("Verdana", Font.PLAIN, 12);
    static int count = 0;
    CellRenderer(boolean taskInfo) {
      if (taskInfo) {
        _labelProc = null;
        setLayout(new GridLayout(1, 3, 5, 5));
        add(_labelTask); add(_labelState); add(_panelProgress);
        _panelProgress.add(_labelProgress); 
      } else {
        _labelProc = new JLabel();
        setLayout(new GridLayout(1, 4, 5, 5));
        add(_labelTask); add(_labelProc); add(_labelState); add(_panelProgress);
        _panelProgress.add(_labelProgress); 
      }
      setBackground(Color.gray);
      setOpaque(true);
    }
    public Component getListCellRendererComponent(JList list, 
        Object value, int index, boolean isSelected, boolean cellHasFocus) {
      if (value.toString().equals("HEADER")) {          
        _labelTask.setFont(headerFont);
        _labelState.setFont(headerFont);
        _labelProgress.setFont(headerFont);
        
        _labelTask.setText(_labelProc==null ? "Task name" : "Replicating");
        _labelState.setText("State");
        _labelProgress.setText("Progress");
        _panelProgress.setBackground(Color.gray);
        setBackground(Color.gray);
        if (_labelProc != null) {
          _labelProc.setFont(headerFont); 
          _labelProc.setText("Processor");
        }        

      } else if (value instanceof DisplayItem) {
        DisplayItem di = (DisplayItem) value;
        _labelTask.setFont(regularFont);
        _labelState.setFont(regularFont);
        _labelProgress.setFont(regularFont);

        _labelTask.setText(di._taskName);
        _labelProgress.setText("");

        setBackground(Color.lightGray);

        if (_labelProc == null) {
          // running|finished|killed|resultDisposed
          if (di.isActive()) {
            _panelProgress.setBackground(Color.green);
            _labelState.setText("executing");
          } else if (di.isExited()) {
            _panelProgress.setBackground(Color.black);
            _labelState.setText("completed");
          } else if (di.isKilled()) { 
            _panelProgress.setBackground(Color.orange);
            _labelState.setText("results disposed");
          } else if (di.isToKill()) {
            _panelProgress.setBackground(Color.red);
            _labelState.setText("dispose result");
          }
        } else {
          // replicating|replicated|procDown|timedOut
          _labelProc.setFont(regularFont);
          _labelProc.setText(di._procName);
          if (di.isReplActive()) { 
            _panelProgress.setBackground(Color.green);
            _labelState.setText("replicating");
          } else if (di.isReplExited()) {
            _panelProgress.setBackground(Color.black);
            _labelState.setText("completed");
          } else if (di.isReplProcDown()) {
            _panelProgress.setBackground(Color.red);
            _labelState.setText("processor down");
          } else if (di.isReplTimedOut()) {
            _panelProgress.setBackground(Color.orange);
            _labelState.setText("timed-out");
          }
        }
      }
      return this;
    }
  }

  // TASK-INFO DISPLAY //////////////////////////////////////////////
  final JPanel _taskPanel;
  final JScrollPane _taskScrollPane;
  final DefaultListModel _taskListModel = new DefaultListModel();
  final JList _taskList = new JList(_taskListModel);
  
  /**
   * initialise the task-info display panel
   * 
   */
  private JScrollPane initTaskPanel() {
    _taskList.setCellRenderer(new CellRenderer(true));
    _taskListModel.addElement("HEADER");
    
    _taskPanel.setLayout(new BorderLayout());
    _taskPanel.add(new JLabel("Task information"), BorderLayout.NORTH);
    _taskPanel.add(_taskList, BorderLayout.CENTER);
    
    return (_taskScrollPane);
  }
  
  // REPLICATING-INFO DISPLAY ///////////////////////////////////////
  final JPanel _replPanel;
  final JScrollPane _replScrollPane;
  final DefaultListModel _replListModel = new DefaultListModel();
  final JList _replList = new JList(_replListModel);
  
  /**
   * initialise the replicating-info display panel
   */
  private JScrollPane initReplPanel() {
    _replList.setCellRenderer(new CellRenderer(false));
    _replListModel.addElement("HEADER");
    
    _replPanel.setLayout(new BorderLayout());
    _replPanel.add(new JLabel("Replication information"), BorderLayout.NORTH);
    _replPanel.add(_replList, BorderLayout.CENTER);
    
    return (_replScrollPane);
  }
  
  ///////////////////////////////////////////////////////////////////
  
  /**
   * This class is used to represent an item that can be displayed
   * 
   * @author Jean-Denis Greze (jg253@cs.columbia.edu)
   * @author Gaurav S. Kc (gskc@cs.columbia.edu)
   */
  static final class DisplayItem {
    final String _taskName;
    final String _procName;
    int _status;
    private static final Map _map = new TreeMap();
    
    /** executing task */
    private static final int ACTIVE = 0;

    /** task completed normally */
    private static final int EXITED = 1;

    /** task completed after had been killed */
    private static final int KILLED = 2;

    /** murder task, ignore results */
    private static final int TOKILL = 3;
    

    /** start monitoring remote task execution */
    private static final int REPL_ACTIVE = 100;
    /** no more monitoring, remote task completed */
    private static final int REPL_EXITED = 101;

    /** alert, remote processor down */
    private static final int REPL_PROC_DOWN = 102;
    /** alert, remote task timed-out */
    private static final int REPL_TIMED_OUT = 103;
    
    
    private DisplayItem(String taskName, String procName) {
      _taskName = taskName;
      _procName = procName;
      _status = ACTIVE;
    }
    
    public String toString() {
      return _taskName + ":" + _procName + ":" + _status;
    }
    
    static DisplayItem createInstance(Version key, String taskName, String procName) {
      if (_map.containsKey(key)) return null;
      _map.put(key, new DisplayItem(taskName, procName));
      return instance(key);
    }
    static DisplayItem instance(Version key) {
      return (DisplayItem) _map.get(key);
    }
    
    void exited() { if (isActive() || isToKill()) _status = EXITED; }
    void killed() { if (isExited()) _status = KILLED; }
    void toKill() { if (isActive()) _status = TOKILL; }
    
    boolean isActive() { return _status == ACTIVE; }
    boolean isExited() { return _status == EXITED; }
    boolean isKilled() { return _status == KILLED; }
    boolean isToKill() { return _status == TOKILL; }
    
    void replActive() { if (isActive()) _status = REPL_ACTIVE; }
    void replExited() { if (isReplActive()) _status = REPL_EXITED; }
    void replProcDown() { if (isReplActive()) _status = REPL_PROC_DOWN; }
    void replTimedOut() { if (isReplActive()) _status = REPL_TIMED_OUT; }
    
    boolean isReplActive() { return _status == REPL_ACTIVE; }
    boolean isReplExited() { return _status == REPL_EXITED; }
    boolean isReplProcDown() { return _status == REPL_PROC_DOWN; }
    boolean isReplTimedOut() { return _status == REPL_TIMED_OUT; }
    
  }

  ///////////////////////////////////////////////////////////////////

  /**
   * Implementation of the general logging interface
   * 
   * @author Jean-Denis Greze (jg253@cs.columbia.edu)
   * @author Gaurav S. Kc (gskc@cs.columbia.edu)
   * 
   * todo:
   *  - implement some sort of persistent storage for log
   */
  final class Logger implements Log {
    
    private String taskName(Version v) {
      if (v == null) return (global_TASKNAME!=null ? global_TASKNAME : new java.util.Date().toString().substring(0, 10));
      NRLProcessData npd = (NRLProcessData) v.data2();
      return (npd != null ? npd.nextTaskName : "npd null?"); // todo: for some reason, npd is null!
    }

    private String procName(Version v) {
      if (v == null) return (global_PROCNAME!=null ? global_PROCNAME : new java.util.Date().toString().substring(0, 10));
      TaskProcessorHandle tph = (TaskProcessorHandle) v.data();
      return (tph != null ? tph.getName() : "tph null?"); // todo: for some reason, tph is null!
    }
    
    /**
     * processor was asked to execute a task
     */
    public void executeTaskLocal(Version v) {
      DisplayItem di = DisplayItem.createInstance(v, taskName(v), "");
      if (di == null) return;
      _taskListModel.addElement(di);
      _taskList.validate(); _taskList.repaint();
    }
    
    /**
     * processor completed executing the task
     */
    public void completedTaskLocal(Version v) {
      DisplayItem di = DisplayItem.instance(v);
      if (di == null) return;
      di.exited();
      _taskList.validate(); _taskList.repaint();
    }
    
    /**
     * processor was asked to stop a running task
     */
    public void stopTaskLocal(Version v) {
      DisplayItem di = DisplayItem.instance(v);
      if (di == null) return;
      di.toKill();
      _taskList.validate(); _taskList.repaint();
    }
    
    /**
     * processor completed executing the task, but it had
     * already been asked to stop the task
     */
    public void ignoreResultsOfStoppedTask(Version v) {
      DisplayItem di = DisplayItem.instance(v);
      if (di == null) return;
      di.killed();
      _taskList.validate(); _taskList.repaint();
    }
    
    /**
     * replicator was asked to monitor task execution by
     * a remote processor
     */
    public void replicatingTask(Version v, Version finalVer) {
      DisplayItem di = DisplayItem.createInstance(v, taskName(finalVer), procName(v));
      if (di == null) return;
      di.replActive();
      _replListModel.addElement(di);
      _replList.validate(); _replList.repaint();
    }
    
    /**
     * replicator does not need to monitor task execution
     * any more since remote processor completed the task
     */
    public void doneReplicatingTask(Version v) {
      DisplayItem di = DisplayItem.instance(v);
      di.replExited();
      _replList.validate(); _replList.repaint();
    }
    
    /**
     * remote processor in charge of task execution is no
     * longer responding
     */
    public void processorDown(Version v) {
      DisplayItem di = DisplayItem.instance(v);
      if (di == null) return;
      di.replProcDown();
      _replList.validate(); _replList.repaint();
    }
    
    /**
     * remote processor in charge of task execution was not
     * able to complete the task in the expected amount
     * of time
     */
    public void taskTimeOut(Version v) {
      DisplayItem di = DisplayItem.instance(v);
      if (di == null) return;
      di.replTimedOut();
      _replList.validate(); _replList.repaint();
    }  

    // processor capabilities
    public void addedCapability(Object o) {
      // find out the type of o, and use it somehow to update the
      // JTable in _procPanel
      _procListModel.addElement(o);
    }
  }
  
  /**
   * main window
   * 
   */
  public static void main(String args[]) {
    new NPortal("NPortal - the processor portal");
  }
  
  private String global_TASKNAME = null;
  private String global_PROCNAME = null;
  private void testing() {
    JPanel panel = new JPanel(new GridLayout(5, 2));
    _pane.addTab("Control panel", null, panel, "for testing purposes");
    JButton b = null;
    
    final JTextField task = new JTextField("hello world"); panel.add(task); task.setMinimumSize(new Dimension(40, 40));
    final JTextField proc = new JTextField("hello world"); panel.add(proc); task.setMinimumSize(new Dimension(40, 40));

    panel.add(b = new JButton("executeTaskLocal"));
    b.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        global_TASKNAME = task.getText(); loggerInstance.executeTaskLocal(null);
      }
    });
    panel.add(b = new JButton("replicatingTask"));
    b.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        global_TASKNAME = task.getText(); global_PROCNAME = proc.getText(); loggerInstance.replicatingTask(null, null);
      }
    });


    panel.add(b = new JButton("completedTaskLocal"));
    b.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        global_TASKNAME = task.getText(); loggerInstance.completedTaskLocal(null);
      }
    });
    panel.add(b = new JButton("doneReplicatingTask"));
    b.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        global_TASKNAME = task.getText(); global_PROCNAME = proc.getText(); loggerInstance.doneReplicatingTask(null);
      }
    });

    panel.add(b = new JButton("stopTaskLocal"));
    b.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        global_TASKNAME = task.getText(); loggerInstance.stopTaskLocal(null);
      }
    });
    panel.add(b = new JButton("processorDown"));
    b.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        global_TASKNAME = task.getText(); global_PROCNAME = proc.getText(); loggerInstance.processorDown(null);
      }
    });

    panel.add(b = new JButton("ignoreResultsOfStoppedTask"));
    b.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        global_TASKNAME = task.getText(); loggerInstance.ignoreResultsOfStoppedTask(null);
      }
    });
    panel.add(b = new JButton("taskTimeOut"));
    b.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        global_TASKNAME = task.getText(); global_PROCNAME = proc.getText(); loggerInstance.taskTimeOut(null);
      }
    });

  }
}
