package workflows.svr1.end;

import wfruntime.*;

/*
The following methods may be used to access data in this realization:
public void setIntField(String paramName, String fieldName, int val);
public void setDoubleField(String paramName, String fieldName, double val);
public void setFloatField(String paramName, String fieldName, float val);
public void setBooleanField(String paramName, String fieldName, boolean val);
public void setLongField(String paramName, String fieldName, long val);
public void setByteField(String paramName, String fieldName, byte val);
public void setShortField(String paramName, String fieldName, short val);
public void setCharField(String paramName, String fieldName, char val);
public int getIntField(String paramName, String fieldName);
public double getDoubleField(String paramName, String fieldName);
public float getFloatField(String paramName, String fieldName);
public boolean getBooleanField(String paramName, String fieldName);
public long getLongField(String paramName, String fieldName);
public byte getByteField(String paramName, String fieldName);
public short getShortField(String paramName, String fieldName);
public char getCharField(String paramName, String fieldName);
public Object getObjField(String paramName, String fieldName);
public void setObjField(String paramName, String fieldName, Object val);

The following data objects are available:

The following permissions are granted:

*/

public class Realization extends AbstractRealization{

  	public void run(){
    try{

      System.out.println("RUNNING END TASK");
      System.out.println("DONE RUNNING END TASK");
      taskMgr().endTask(null);
    }
    catch(Exception e){

      taskMgr().endTask(e);
    }
  }

}
