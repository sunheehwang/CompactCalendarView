package com.github.sundeepk.compactcalendarview;

import com.github.sundeepk.compactcalendarview.comparators.EventComparator;
import com.github.sundeepk.compactcalendarview.domain.Event;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class EventsContainer {

    private Map<String, List<Events>> eventsByMonthAndYearMap = new HashMap<>();
    private Comparator<Event> eventsComparator = new EventComparator();
    private Calendar eventsCalendar;
    private Calendar calendar;

    public EventsContainer(Calendar eventsCalendar) {
        this.eventsCalendar = eventsCalendar;
        this.calendar = Calendar.getInstance();
    }

    void addEvent(Event event) {
        eventsCalendar.setTimeInMillis(event.getTimeInMillis());
        String key = getKeyForCalendarEvent(eventsCalendar);
        List<Events> eventsForMonth = eventsByMonthAndYearMap.get(key);
        if (eventsForMonth == null) {
            eventsForMonth = new ArrayList<>();
        }
        Events eventsForTargetDay = getEventDayEvent(event.getTimeInMillis());
        if (eventsForTargetDay == null) {
            List<Event> events = new ArrayList<>();
            events.add(event);
            eventsForMonth.add(new Events(event.getTimeInMillis(), events));
        } else {
            eventsForTargetDay.getEvents().add(event);
        }
        eventsByMonthAndYearMap.put(key, eventsForMonth);
    }

    void removeAllEvents() {
        eventsByMonthAndYearMap.clear();
    }

    void addEvents(List<Event> events) {
        int count = events.size();
        for (int i = 0; i < count; i++) {
            addEvent(events.get(i));
        }
    }

    List<Event> getEventsFor(long epochMillis) {
        Events events = getEventDayEvent(epochMillis);
        if (events == null) {
            return new ArrayList<>();
        } else {
            return events.getEvents();
        }
    }

    List<Events> getEventsForMonthAndYear(int month, int year){
        return eventsByMonthAndYearMap.get(year + "_" + month);
    }

    List<Events> getEventsForMonthAndYearAndWeekOfYear(Calendar selectedCalendar, int firstDayOfWeek){

        int year = selectedCalendar.get(Calendar.YEAR);
        int month = selectedCalendar.get(Calendar.MONTH);
        int day = selectedCalendar.get(Calendar.DAY_OF_MONTH);
        int dayOfWeek = selectedCalendar.get(Calendar.DAY_OF_WEEK);
        int maxOfMonth = selectedCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        int weekOfYear = selectedCalendar.get(Calendar.WEEK_OF_YEAR);
        int firstDay = day - (dayOfWeek - firstDayOfWeek);
        int diff = maxOfMonth - firstDayOfWeek;

        List<Events> eventsList = eventsByMonthAndYearMap.get(year + "_" + month);
        if (eventsList == null) {
            eventsList = new ArrayList<>();
        } else {
            eventsList = new ArrayList<>(eventsList);
        }

        if (firstDay < 0) {
            // previous week
            int previousYear = (month == 1)? year - 1: year;
            int previousMonth = (month == 1)? 12: month -1;

            List<Events> previousEventsList = eventsByMonthAndYearMap.get(previousYear + "_" + previousMonth);
            if (previousEventsList != null) {
                eventsList.addAll(0, previousEventsList);
            }
        } else if (diff > 7) {
            // next week
            int nextYear = (month == 12)? year + 1: year;
            int nextMonth = (month == 12)? 1: month +1;

            List<Events> nextEventsList = eventsByMonthAndYearMap.get(nextYear + "_" + nextMonth);
            if (nextEventsList != null) {
                eventsList.addAll(0, nextEventsList);
            }
        }


        int fromIndex = -1;
        int toIndex = -1;
        int size = eventsList == null ? 0: eventsList.size();
        for (int i = 0; i < size; i++) {
            Events events = eventsList.get(i);
            calendar.setTimeInMillis(events.getTimeInMillis());
            if (weekOfYear == calendar.get(Calendar.WEEK_OF_YEAR)) {
                if (fromIndex == -1) {
                    fromIndex = i;
                    toIndex = i;
                } else {
                    toIndex = i;
                }
            }
        }
        return (fromIndex == -1)? Collections.<Events>emptyList(): eventsList.subList(fromIndex, toIndex+1);
    }


    List<Event> getEventsForMonth(long eventTimeInMillis){
        eventsCalendar.setTimeInMillis(eventTimeInMillis);
        String keyForCalendarEvent = getKeyForCalendarEvent(eventsCalendar);
        List<Events> events = eventsByMonthAndYearMap.get(keyForCalendarEvent);
        List<Event> allEventsForMonth = new ArrayList<>();
        if (events != null) {
            for(Events eve : events){
                if (eve != null) {
                    allEventsForMonth.addAll(eve.getEvents());
                }
            }
        }
        Collections.sort(allEventsForMonth, eventsComparator);
        return allEventsForMonth;
    }

    private Events getEventDayEvent(long eventTimeInMillis){
        eventsCalendar.setTimeInMillis(eventTimeInMillis);
        int dayInMonth = eventsCalendar.get(Calendar.DAY_OF_MONTH);
        String keyForCalendarEvent = getKeyForCalendarEvent(eventsCalendar);
        List<Events> eventsForMonthsAndYear = eventsByMonthAndYearMap.get(keyForCalendarEvent);
        if (eventsForMonthsAndYear != null) {
            for (Events events : eventsForMonthsAndYear) {
                eventsCalendar.setTimeInMillis(events.getTimeInMillis());
                int dayInMonthFromCache = eventsCalendar.get(Calendar.DAY_OF_MONTH);
                if (dayInMonthFromCache == dayInMonth) {
                    return events;
                }
            }
        }
        return null;
    }

    void removeEventByEpochMillis(long epochMillis) {
        eventsCalendar.setTimeInMillis(epochMillis);
        int dayInMonth = eventsCalendar.get(Calendar.DAY_OF_MONTH);
        String key = getKeyForCalendarEvent(eventsCalendar);
        List<Events> eventsForMonthAndYear = eventsByMonthAndYearMap.get(key);
        if (eventsForMonthAndYear != null) {
            Iterator<Events> calendarDayEventIterator = eventsForMonthAndYear.iterator();
            while (calendarDayEventIterator.hasNext()) {
                Events next = calendarDayEventIterator.next();
                eventsCalendar.setTimeInMillis(next.getTimeInMillis());
                int dayInMonthFromCache = eventsCalendar.get(Calendar.DAY_OF_MONTH);
                if (dayInMonthFromCache == dayInMonth) {
                    calendarDayEventIterator.remove();
                    return;
                }
            }
        }
    }

    void removeEvent(Event event) {
        eventsCalendar.setTimeInMillis(event.getTimeInMillis());
        String key = getKeyForCalendarEvent(eventsCalendar);
        List<Events> eventsForMonthAndYear = eventsByMonthAndYearMap.get(key);
        if (eventsForMonthAndYear != null) {
            for(Events events : eventsForMonthAndYear){
                int indexOfEvent = events.getEvents().indexOf(event);
                if (indexOfEvent >= 0) {
                    events.getEvents().remove(indexOfEvent);
                    return;
                }
            }
        }
    }

    void removeEvents(List<Event> events) {
        int count = events.size();
        for (int i = 0; i < count; i++) {
            removeEvent(events.get(i));
        }
    }

    //E.g. 4 2016 becomes 2016_4
    private String getKeyForCalendarEvent(Calendar cal) {
        return cal.get(Calendar.YEAR) + "_" + cal.get(Calendar.MONTH);
    }

}
