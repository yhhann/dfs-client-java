package com.jingoal.dfsclient;

public class LackofDomainOrBizNameException extends InvalidArgumentException{
  private static final long serialVersionUID = 2338571935221717906L;

  public LackofDomainOrBizNameException(){
    super();
  }

  public LackofDomainOrBizNameException(String msg){
    super(msg);
  }

  public LackofDomainOrBizNameException(Throwable cause){
    super(cause);
  }

  public LackofDomainOrBizNameException(final String msg, final Throwable cause) {
    super(msg, cause);
  }
}