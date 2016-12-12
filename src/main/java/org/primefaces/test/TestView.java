package org.primefaces.test;

import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.joda.time.DateMidnight;
import org.primefaces.component.timeline.TimelineUpdater;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.timeline.TimelineLazyLoadEvent;
import org.primefaces.event.timeline.TimelineRangeEvent;
import org.primefaces.event.timeline.TimelineSelectEvent;
import org.primefaces.model.CheckboxTreeNode;
import org.primefaces.model.TreeNode;
import org.primefaces.model.timeline.TimelineEvent;
import org.primefaces.model.timeline.TimelineModel;
import org.primefaces.test.model.TestObject;


@ManagedBean(name = "testView")
@ViewScoped
public class TestView implements Serializable {
    
	private static final long serialVersionUID = -3439928289992712730L;
	private TimelineModel timeline;
	private Date startDate;
	private Date endDate;
	private Date minDate;
	private Date maxDate;

	private long zoomMin;
	private long zoomMax;

	protected TestObject searchExample;
	protected TestObject selectedTestObject;

	private static final long ZOOM_ONE_DAY = 1000L * 60 * 60 * 24;
	private static final long ZOOM_ONE_YEAR = 1000L * 60 * 60 * 24 * 31 * 12;
	private static final int DAYS_IN_MONTH = 30;
	private static final int MAX_YEARS = 5;
	
	private Date searchStartDate;
	private Date searchEndDate;

	private List<TestObject> mockedObjects;
	
	private static final String TIMELINE_JSF_ID = ":timelineForm:timeline";
	
	@PostConstruct
	public void init() {
		
		try {
			createMockedObjects();			
		} catch (Exception e) {
			System.out.println("error parsing dates");
		}
		
		setTimelineDates();
		
		timeline = new TimelineModel();
		
		loadUserPreferences();
		
	}
	
	private void createMockedObjects() throws ParseException{
		
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-M-yyyy");
		
		mockedObjects = new ArrayList<TestObject>();
		mockedObjects.add(new TestObject(1, "First", "First Mocked Object", simpleDateFormat.parse("30-01-2014")));
		mockedObjects.add(new TestObject(2, "Second", "Second Mocked Object", simpleDateFormat.parse("24-02-2015")));
		mockedObjects.add(new TestObject(3, "Third", "Third Mocked Object", simpleDateFormat.parse("07-08-2016")));
		mockedObjects.add(new TestObject(4, "Fourth", "Fourth Mocked Object", simpleDateFormat.parse("09-11-2016")));
		mockedObjects.add(new TestObject(5, "Fifth", "Fifth Mocked Object", simpleDateFormat.parse("30-01-2017")));

		

	}

	
	public boolean checkTooManyEvents(int maxEvents){
		int count = 0;
		for (TimelineEvent event : timeline.getEvents()){
			if (startDate.before(event.getStartDate()) && endDate.after(event.getStartDate()) ){
				count++;
			}
			if (count > maxEvents){
				return true;
			}
		}
		
		return false;
	}

	public void onRangeChanged(TimelineRangeEvent timelineRangeEvent) {
		startDate = timelineRangeEvent.getStartDate();
		endDate = timelineRangeEvent.getEndDate();
	}


	@SuppressWarnings("unchecked")
	private void loadUserPreferences(){
		
		if(searchExample == null){
			searchExample = new TestObject();
		}
		
	}

	public void loadEvents() {
		TimelineUpdater timelineUpdater = TimelineUpdater.getCurrentInstance(TIMELINE_JSF_ID);
		timeline.clear(timelineUpdater);
		List<TimelineEvent> events = loadTimelineEvents(startDate, endDate);
		timeline.addAll(events, timelineUpdater);
	}

	public void resetFields() {
		setTimelineDates();
		searchExample = new TestObject();
		searchEndDate = null;
		searchStartDate = null;
		setMinDate(null);
		setMaxDate(null);
		loadEvents();	
	}

	private void setTimelineDates() {
		setTimeLinesDates30daysDiference(new Date(), new Date());
		setZoomMin(ZOOM_ONE_DAY * DAYS_IN_MONTH);
		setZoomMax(ZOOM_ONE_YEAR * MAX_YEARS);
	}

	private void setTimeLinesDates30daysDiference(Date dateFromToStart, Date dateFromToEnd) {
		DateMidnight dateMidnightStart = new DateMidnight(dateFromToStart);
		DateMidnight dateMidnightEnd = new DateMidnight(dateFromToEnd);
		setStartDate(dateMidnightStart.minusDays(30).toDate());
		setEndDate(dateMidnightEnd.plusDays(30).toDate());
	}

	public void onTestObjectSelect(TimelineSelectEvent event) {
		TimelineEvent selectedEvent = event.getTimelineEvent();
		selectedTestObject = (TestObject) selectedEvent.getData();
	}
	
	

	public void onLazyLoad(TimelineLazyLoadEvent event) {
		TimelineUpdater timelineUpdater = TimelineUpdater.getCurrentInstance(TIMELINE_JSF_ID);
		timeline.addAll(loadTimelineEvents(event.getStartDateFirst(), event.getEndDateFirst()), timelineUpdater);
		
		if (startDate == null || startDate.after(event.getStartDateFirst())) {
			startDate = event.getStartDateFirst();
		}
		if (endDate == null || endDate.before(event.getEndDateFirst())) {
			endDate = event.getEndDateFirst();
		}

		if (event.hasTwoRanges()) {
			timeline.addAll(loadTimelineEvents(event.getStartDateSecond(), event.getEndDateSecond()), timelineUpdater);

			if (startDate == null || startDate.after(event.getStartDateSecond())) {
				startDate = event.getStartDateSecond();
			}
			if (endDate == null || endDate.before(event.getEndDateSecond())) {
				endDate = event.getEndDateSecond();
			}
		}

	}

	public List<TimelineEvent> loadTimelineEvents(Date startDate, Date endDate) {

		List<TestObject> testObjects = this.loadTimelineData(startDate, endDate, searchExample);
		
		List<TimelineEvent> listEvents = new ArrayList<TimelineEvent>();
		for (TestObject testObject : testObjects) {

			TimelineEvent currEvent = new TimelineEvent(testObject,testObject.getDate(), false, null, "testObjectClass");
			if (!timeline.getEvents().contains(currEvent)) {
				listEvents.add(currEvent);
			}
		}
		return listEvents;
	}
	
	public List<TestObject> loadTimelineData(Date startDate, Date endDate, TestObject example) {

	
		List<TestObject> testObjectList = new ArrayList<TestObject>();
		
		for(TestObject testObject : mockedObjects){
			
			if(testObject.getDate().compareTo(startDate) > 0 && testObject.getDate().compareTo(endDate) < 0 ){
				
				testObjectList.add(testObject);
				
			}
			
		}
		
		return testObjectList;
	}



	public void manageDateFrom(SelectEvent event) {

		setMinDate((Date) event.getObject());

	}

	public void manageDateUntil(SelectEvent event) {

		setMaxDate((Date) event.getObject());

	}

	
	public Date getSearchStartDate() {
		return searchStartDate != null ? (Date) searchStartDate.clone() : null;
	}

	public void setSearchStartDate(Date searchStartDate) {
		this.searchStartDate = searchStartDate == null ? null : new Date(searchStartDate.getTime());
	}
	
	public Date getSearchEndDate() {
		return searchEndDate != null ? (Date) searchEndDate.clone() : null;
	}

	public void setSearchEndDate(Date searchEndDate) {
		this.searchEndDate = searchEndDate == null ? null : new Date(searchEndDate.getTime());
	}

	public Date getStartDate() {
		return startDate != null ? (Date) startDate.clone() : null;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate == null ? null : new Date(startDate.getTime());
	}
	
	public Date getEndDate() {
		return endDate != null ? (Date) endDate.clone() : null;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate == null ? null : new Date(endDate.getTime());
	}

	public TimelineModel getTimeline() {
		return timeline;
	}

	public void setTimeline(TimelineModel timeline) {
		this.timeline = timeline;
	}
	
	public Date getMinDate() {
		return minDate != null ? (Date) minDate.clone() : null;
	}

	public void setMinDate(Date minDate) {
		this.minDate = minDate == null ? null : new Date(minDate.getTime());
	}

	public long getZoomMin() {
		return zoomMin;
	}

	public void setZoomMin(long zoomMin) {
		this.zoomMin = zoomMin;
	}

	public long getZoomMax() {
		return zoomMax;
	}

	public void setZoomMax(long zoomMax) {
		this.zoomMax = zoomMax;
	}

	public TestObject getSearchExample() {
		return searchExample;
	}

	public void setSearchExample(TestObject searchExample) {
		this.searchExample = searchExample;
	}
	
	public Date getMaxDate() {
		return maxDate != null ? (Date) maxDate.clone() : null;
	}

	public void setMaxDate(Date maxDate) {
		this.maxDate = maxDate == null ? null : new Date(maxDate.getTime());
	}
  
}
