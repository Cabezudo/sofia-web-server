package net.cabezudo.sofia.core.company;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.cabezudo.hayquecomer.food.Categories;
import net.cabezudo.hayquecomer.food.CategoryHours;
import net.cabezudo.hayquecomer.food.category.Category;
import net.cabezudo.json.JSONPair;
import net.cabezudo.json.values.JSONObject;
import net.cabezudo.sofia.core.exceptions.SofiaRuntimeException;
import net.cabezudo.sofia.core.languages.Language;
import net.cabezudo.sofia.core.schedule.AbstractTime;
import net.cabezudo.sofia.core.schedule.Day;
import net.cabezudo.sofia.core.schedule.EndEvent;
import net.cabezudo.sofia.core.schedule.Event;
import net.cabezudo.sofia.core.schedule.OpenTime;
import net.cabezudo.sofia.core.schedule.OpenTimes;
import net.cabezudo.sofia.core.schedule.StartEvent;
import net.cabezudo.sofia.core.schedule.TimeEvents;
import net.cabezudo.sofia.core.schedule.Times;

/**
 * @author <a href="http://cabezudo.net">Esteban Cabezudo</a>
 * @version 0.01.00, 2020.09.18
 */
public class BusinessHours {

  private Boolean openNow;
  private int dayOfWeek;
  private int tomorrowDayOfWeek;
  private final OpenTimes todayEvents = new OpenTimes();
  private final OpenTimes tomorrowEvents = new OpenTimes();
  private Event todayOpenAt = null;
  private Event tomorrowOpenAt = null;
  private Day todayName;
  private Day tomorrowName;
  private final Times times = new Times();
  private Instant instant;
  private boolean calculated = false;

  public BusinessHours(List<AbstractTime> timeList) {
    for (AbstractTime time : timeList) {
      times.add(time);
    }
  }

  public void calculateFor(int timezoneOffset) {
    if (instant != null) {
      throw new SofiaRuntimeException("Already calculated for offset " + timezoneOffset);
    }
    instant = Instant.now();
    OffsetDateTime now = instant.atOffset(ZoneOffset.ofHoursMinutes(-timezoneOffset / 60, -timezoneOffset % 60));
    dayOfWeek = now.getDayOfWeek().getValue();
    todayName = new Day(dayOfWeek);
    tomorrowDayOfWeek = now.getDayOfWeek().plus(1).getValue();
    tomorrowName = new Day(tomorrowDayOfWeek);

    TimeEvents temporalEvents = createEvents();
    cleanOverlapedEvents(temporalEvents);

    int hour = now.getHour();
    int minutes = now.getMinute();
    int time = ((hour * 60) + minutes) * 60;

    Boolean isOpen = null;

    for (OpenTime event : todayEvents) {
      isOpen = false;
      if (event.isOpen(time)) {
        isOpen = true;
        break;
      }
      if (todayOpenAt == null && event.getStart().getHour().getTime() > time) {
        todayOpenAt = event.getStart();
      }
    }
    openNow = isOpen;
    for (OpenTime event : tomorrowEvents) {
      if (tomorrowOpenAt == null) {
        tomorrowOpenAt = event.getStart();
      }
    }
  }

  public Boolean isOpen() {
    return openNow;
  }

  public Map<Category, CategoryHours> getHoursById(Categories categories) {
    Map<Category, CategoryHours> map = new TreeMap<>();

    for (Category category : categories) {
      TimeEvents temporalEvents = createEvents();
      OpenTimes cleanTimes = cleanOverlapedEventsById(temporalEvents);
      map.put(category, new CategoryHours(cleanTimes));
    }
    return map;
  }

  public JSONObject toJSONTree() {
    JSONObject jsonObject = new JSONObject();
    jsonObject.add(new JSONPair("businessHours", times.toJSONTree()));
    return jsonObject;
  }

  public JSONObject toJSONForRestaurantList(Language language, int timezoneOffset) {
    JSONObject jsonObject = new JSONObject();
    JSONObject jsonToday = new JSONObject();
    jsonToday.add(new JSONPair("shortName", todayName.getShortName(language)));
    jsonToday.add(new JSONPair("name", todayName.getName(language)));
    jsonToday.add(new JSONPair("isOpen", openNow));
    jsonToday.add(new JSONPair("openAt", todayOpenAt == null ? null : todayOpenAt.getHour().toHHmm()));
    jsonToday.add(new JSONPair("times", todayEvents.toJSONTree()));
    jsonObject.add(new JSONPair("today", jsonToday));

    JSONObject jsonTomorrow = new JSONObject();
    jsonTomorrow.add(new JSONPair("shortName", tomorrowName.getShortName(language)));
    jsonTomorrow.add(new JSONPair("name", tomorrowName.getName(language)));
    jsonTomorrow.add(new JSONPair("openAt", tomorrowOpenAt == null ? null : tomorrowOpenAt.getHour().toHHmm()));
    jsonTomorrow.add(new JSONPair("times", tomorrowEvents.toJSONTree()));
    jsonObject.add(new JSONPair("tomorrow", jsonTomorrow));

    Date closedUntil = null;
    if ((openNow != null && !openNow) && todayOpenAt == null && tomorrowOpenAt == null) {
      closedUntil = calculateNextOpen();
    }
    jsonObject.add(new JSONPair("closedUntil", closedUntil));
    return jsonObject;
  }

  private TimeEvents createEvents() {
    TimeEvents events = new TimeEvents();

    for (AbstractTime time : times) {
      if (time.dayIs(dayOfWeek) || time.dayIs(tomorrowDayOfWeek)) {
        events.add(new StartEvent(time.getIndex(), time.getStart()));
        events.add(new EndEvent(time.getIndex(), time.getEnd()));
      }
    }
    return events;
  }

  private void cleanOverlapedEvents(TimeEvents temporalEvents) {
    Event lastEvent = null;
    int open = 0;

    for (Event event : temporalEvents) {
      if (event.isStart()) {
        if (open == 0) {
          lastEvent = event;
        }
        open++;
      }
      if (event.isEnd()) {
        open--;
        if (open == 0) {
          distributeEvents(lastEvent, event);
        }
      }
    }
  }

  private void distributeEvents(Event lastEvent, Event event) {
    if (dayOfWeek == event.getDay()) {
      todayEvents.add(event.getDay(), lastEvent, event);
    } else {
      tomorrowEvents.add(event.getDay(), lastEvent, event);
    }
  }

  private Date calculateNextOpen() {
    // TODO calculate the next day open
    return null;
  }

  private OpenTimes cleanOverlapedEventsById(TimeEvents temporalEvents) {
    OpenTimes cleanTimes = new OpenTimes();
    Event lastEvent = null;
    int open = 0;

    for (Event event : temporalEvents) {
      if (event.isStart()) {
        if (open == 0) {
          lastEvent = event;
        }
        open++;
      }
      if (event.isEnd()) {
        open--;
        if (open == 0) {
          cleanTimes.add(event.getDay(), lastEvent, event);
        }
      }
    }
    return cleanTimes;
  }
}
