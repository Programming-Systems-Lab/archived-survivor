/**
 * @author Gaurav S. Kc [gskc@cs.columbia.edu], March 2002
 * 
*/

package psl.survivor.demo;

import javax.swing.JOptionPane;

/**
 * GUI-window to be used for demonstration purposes
 */
public class Input {
  public static String getInput(String prompt) {
    return JOptionPane.showInputDialog(null, prompt, "Query", JOptionPane.QUESTION_MESSAGE);
  }
  public static void display(String message) {
    JOptionPane.showMessageDialog(null, message, "Message", JOptionPane.INFORMATION_MESSAGE);
  }
}