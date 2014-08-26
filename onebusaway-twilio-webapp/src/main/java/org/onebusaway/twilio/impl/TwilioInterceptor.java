package org.onebusaway.twilio.impl;


import java.util.Map;

import org.apache.struts2.interceptor.SessionAware;
import org.onebusaway.presentation.impl.users.XWorkRequestAttributes;
import org.onebusaway.twilio.services.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;

public class TwilioInterceptor extends AbstractInterceptor {

  private static Logger _log = LoggerFactory.getLogger(TwilioInterceptor.class);
        
  private SessionManager _sessionManager;
  private String _phoneNumberParameterName = "From";
  
  public void setPhoneNumberParameterName(String phoneNumberParameterName) {
    _phoneNumberParameterName = phoneNumberParameterName;
  }
  
  @Autowired
  public void setSessionManager(SessionManager sessionManager) {
    _sessionManager = sessionManager;
  }

  
  @Override
  public String intercept(ActionInvocation invocation) throws Exception {
    ActionContext context = invocation.getInvocationContext();
    _log.debug("in with params=" + context.getParameters() + " and session=" + context.getSession());
    Map<String, Object> parameters = context.getParameters();

    Object phoneNumber = parameters.get(_phoneNumberParameterName);
    if (phoneNumber == null) {
      return invocation.invoke();
    }

    if (phoneNumber instanceof String[]) {
      String[] values = (String[]) phoneNumber;
      if (values.length == 0)
        return invocation.invoke();
      phoneNumber = values[0];
    }

    String sessionId = phoneNumber.toString();
    Map<String, Object> persistentSession = _sessionManager.getContext(sessionId);
    _log.debug("remapping sesssion to " + persistentSession);
    Map<String, Object> originalSession = context.getSession();
    context.setSession(persistentSession);

    
    XWorkRequestAttributes attributes = new XWorkRequestAttributes(context,
        sessionId);
    RequestAttributes originalAttributes = RequestContextHolder.getRequestAttributes();
    RequestContextHolder.setRequestAttributes(attributes);

    Object action = invocation.getAction();
    if (action instanceof SessionAware)
      ((SessionAware) action).setSession(persistentSession);

    try {
      return invocation.invoke();
    } finally {
      RequestContextHolder.setRequestAttributes(originalAttributes);
      context.setSession(originalSession);
    }
  }
}