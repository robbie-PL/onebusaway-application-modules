/**
 * Copyright (C) 2016 Cambridge Systematics, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onebusaway.enterprise.webapp.actions.rss;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts2.ServletActionContext;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.onebusaway.enterprise.webapp.actions.OneBusAwayEnterpriseActionSupport;
import org.onebusaway.enterprise.webapp.actions.status.model.StatusGroup;
import org.onebusaway.enterprise.webapp.actions.status.model.StatusItem;
import org.onebusaway.enterprise.webapp.actions.status.service.StatusProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;

@Results({@Result(type = "rss", params={"feedName", "feed", "feedType", "rss_2.0"})})
public class MonitoringAlertsUpdateAction extends OneBusAwayEnterpriseActionSupport {
	  private static Logger _log = LoggerFactory.getLogger(MonitoringAlertsUpdateAction.class);
	  private static final long serialVersionUID = 1L;

	  private SyndFeed _feed;

	  @Autowired
	  private StatusProvider _statusProvider;

	  public SyndFeed getFeed() {
	    return _feed;
	  }

	  @Override
	  public String execute() {
	    _feed = new SyndFeedImpl();
	    StringBuilder title = new StringBuilder();
	    title.append("OneBusAway System Monitoring");

	    HttpServletRequest request = ServletActionContext.getRequest();

	    StringBuilder b = new StringBuilder();
	    b.append("http://");
	    b.append(request.getServerName());
	    if (request.getServerPort() != 80)
	      b.append(":").append(request.getServerPort());
	    if (request.getContextPath() != null)
	      b.append(request.getContextPath());
	    String baseUrl = b.toString();

	    _feed.setTitle(title.toString());
	    _feed.setLink(baseUrl);
	    _feed.setDescription("System Monitoring");

	    List<SyndEntry> entries = new ArrayList<SyndEntry>();

	    // Add Icinga Alerts
	    SyndEntry icingaEntry = new SyndEntryImpl();
	    SyndContent icingaContent = new SyndContentImpl();
	    StatusGroup icingaGroup = _statusProvider.getIcingaStatus();
	    StatusGroup icingaProblems = new StatusGroup();
	    icingaProblems.setTitle(icingaGroup.getTitle());
	    // Only report items where status is not "OK"
	    for (StatusItem icingaItem : icingaGroup.getItems()) {
	      if (icingaItem.getStatus() != StatusItem.Status.OK) {
	        icingaProblems.addItem(icingaItem);
	      }
	    }
	    if (icingaProblems.getItems().size() == 0) {
	      icingaEntry = new SyndEntryImpl();
	      icingaEntry.setTitle("All systems operational");
	      entries.add(icingaEntry);
	    } else {
	      for (StatusItem icingaItem : icingaProblems.getItems()) {
	        icingaEntry = new SyndEntryImpl();
	        icingaEntry.setTitle(icingaItem.getTitle());
	        icingaContent = new SyndContentImpl();
	        icingaContent.setValue(icingaItem.getStatus() + ": " + icingaItem.getDescription());
	        icingaEntry.setDescription(icingaContent);
	        entries.add(icingaEntry);
	      }
	    }

	    _feed.setEntries(entries);
	    return SUCCESS;
	  }
}
