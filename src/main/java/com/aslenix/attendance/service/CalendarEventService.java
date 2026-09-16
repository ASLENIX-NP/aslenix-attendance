package com.aslenix.attendance.service;

import com.aslenix.attendance.entity.CalendarEvent;
import com.aslenix.attendance.repository.CalendarEventRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CalendarEventService {

    private final CalendarEventRepository calendarEventRepository;

    public CalendarEventService(CalendarEventRepository calendarEventRepository) {
        this.calendarEventRepository = calendarEventRepository;
    }

    public List<CalendarEvent> getEventsForBsMonth(int bsYear, int bsMonth) {
        return calendarEventRepository.findByBsYearAndBsMonthOrderByBsDayAsc(bsYear, bsMonth);
    }

    public List<CalendarEvent> getAllEvents() {
        return calendarEventRepository.findAllByOrderByBsDateAsc();
    }

    public CalendarEvent addEvent(String title, String bsDate, boolean isHoliday, String category) {
        if (title == null || title.trim().isEmpty() || bsDate == null || bsDate.trim().isEmpty()) {
            throw new IllegalArgumentException("Title and BS date are required");
        }

        String[] parts = bsDate.trim().split("-");
        int year = 2083;
        int month = 1;
        int day = 1;

        if (parts.length >= 3) {
            try {
                year = Integer.parseInt(parts[0]);
                month = Integer.parseInt(parts[1]);
                day = Integer.parseInt(parts[2]);
            } catch (NumberFormatException ignored) {}
        }

        CalendarEvent event = new CalendarEvent(
                title.trim(),
                bsDate.trim(),
                year,
                month,
                day,
                isHoliday,
                (category != null && !category.trim().isEmpty()) ? category.trim() : "GOVERNMENT"
        );

        return calendarEventRepository.save(event);
    }

    public boolean deleteEvent(Long id) {
        if (calendarEventRepository.existsById(id)) {
            calendarEventRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @PostConstruct
    public void seedOfficialGovernmentEvents() {
        // List of official events matching the user's reference design
        List<CalendarEvent> seeds = new ArrayList<>();

        // Bhadra (Month 5) - exactly matching the picture
        seeds.add(new CalendarEvent("गुरु पूर्णिमा", "2083-05-13", 2083, 5, 13, false, "GOVERNMENT"));
        seeds.add(new CalendarEvent("खीर खाने दिन", "2083-05-15", 2083, 5, 15, false, "GOVERNMENT"));
        seeds.add(new CalendarEvent("अन्तर्राष्ट्रिय युवा दिवस", "2083-05-27", 2083, 5, 27, false, "GOVERNMENT"));
        seeds.add(new CalendarEvent("श्रीकृष्ण जन्माष्टमी", "2083-05-08", 2083, 5, 8, true, "GOVERNMENT"));
        seeds.add(new CalendarEvent("हरितालिका तीज", "2083-05-16", 2083, 5, 16, true, "GOVERNMENT"));
        seeds.add(new CalendarEvent("ऋषि पञ्चमी", "2083-05-18", 2083, 5, 18, false, "GOVERNMENT"));
        seeds.add(new CalendarEvent("इन्द्रजात्रा", "2083-05-28", 2083, 5, 28, true, "GOVERNMENT"));

        // Month 2 as in screenshot date format (2083-02-13, 2083-02-15, 2083-02-27)
        seeds.add(new CalendarEvent("गुरु पूर्णिमा", "2083-02-13", 2083, 2, 13, false, "GOVERNMENT"));
        seeds.add(new CalendarEvent("खीर खाने दिन", "2083-02-15", 2083, 2, 15, false, "GOVERNMENT"));
        seeds.add(new CalendarEvent("अन्तर्राष्ट्रिय युवा दिवस", "2083-02-27", 2083, 2, 27, false, "GOVERNMENT"));

        // Baisakh (Month 1)
        seeds.add(new CalendarEvent("नयाँ वर्ष २०८३", "2083-01-01", 2083, 1, 1, true, "GOVERNMENT"));
        seeds.add(new CalendarEvent("अन्तर्राष्ट्रिय श्रमिक दिवस", "2083-01-18", 2083, 1, 18, true, "GOVERNMENT"));
        seeds.add(new CalendarEvent("बुद्ध जयन्ती / उभौली", "2083-01-28", 2083, 1, 28, true, "GOVERNMENT"));

        // Jestha (Month 2)
        seeds.add(new CalendarEvent("गणतन्त्र दिवस", "2083-02-15", 2083, 2, 15, true, "GOVERNMENT"));

        // Ashadh (Month 3)
        seeds.add(new CalendarEvent("राष्ट्रिय धान दिवस", "2083-03-15", 2083, 3, 15, false, "GOVERNMENT"));

        // Shrawan (Month 4)
        seeds.add(new CalendarEvent("नाग पञ्चमी", "2083-04-24", 2083, 4, 24, false, "GOVERNMENT"));
        seeds.add(new CalendarEvent("जनै पूर्णिमा / रक्षाबन्धन", "2083-04-31", 2083, 4, 31, true, "GOVERNMENT"));

        // Ashwin (Month 6) - Dashain
        seeds.add(new CalendarEvent("संविधान दिवस", "2083-06-03", 2083, 6, 3, true, "GOVERNMENT"));
        seeds.add(new CalendarEvent("घटस्थापना", "2083-06-08", 2083, 6, 8, true, "GOVERNMENT"));
        seeds.add(new CalendarEvent("फूलपाती", "2083-06-14", 2083, 6, 14, true, "GOVERNMENT"));
        seeds.add(new CalendarEvent("महाअष्टमी", "2083-06-15", 2083, 6, 15, true, "GOVERNMENT"));
        seeds.add(new CalendarEvent("महानवमी", "2083-06-16", 2083, 6, 16, true, "GOVERNMENT"));
        seeds.add(new CalendarEvent("विजया दशमी", "2083-06-17", 2083, 6, 17, true, "GOVERNMENT"));

        // Kartik (Month 7) - Tihar
        seeds.add(new CalendarEvent("लक्ष्मी पूजा", "2083-07-08", 2083, 7, 8, true, "GOVERNMENT"));
        seeds.add(new CalendarEvent("गोवर्धन पूजा / म्ह पूजा", "2083-07-09", 2083, 7, 9, true, "GOVERNMENT"));
        seeds.add(new CalendarEvent("भाइटीका", "2083-07-10", 2083, 7, 10, true, "GOVERNMENT"));
        seeds.add(new CalendarEvent("छठ पर्व", "2083-07-15", 2083, 7, 15, true, "GOVERNMENT"));

        // Mangsir (Month 8)
        seeds.add(new CalendarEvent("उधौली पर्व / योमरी पुन्ही", "2083-08-18", 2083, 8, 18, true, "GOVERNMENT"));

        // Poush (Month 9)
        seeds.add(new CalendarEvent("तमु ल्होसार", "2083-09-15", 2083, 9, 15, true, "GOVERNMENT"));

        // Magh (Month 10)
        seeds.add(new CalendarEvent("माघे संक्रान्ति", "2083-10-01", 2083, 10, 1, true, "GOVERNMENT"));
        seeds.add(new CalendarEvent("शहीद दिवस", "2083-10-16", 2083, 10, 16, false, "GOVERNMENT"));
        seeds.add(new CalendarEvent("सोनाम ल्होसार", "2083-10-18", 2083, 10, 18, true, "GOVERNMENT"));

        // Falgun (Month 11)
        seeds.add(new CalendarEvent("प्रजातन्त्र दिवस", "2083-11-07", 2083, 11, 7, true, "GOVERNMENT"));
        seeds.add(new CalendarEvent("महाशिवरात्रि", "2083-11-13", 2083, 11, 13, true, "GOVERNMENT"));
        seeds.add(new CalendarEvent("अन्तर्राष्ट्रिय महिला दिवस", "2083-11-24", 2083, 11, 24, true, "GOVERNMENT"));
        seeds.add(new CalendarEvent("फागु पूर्णिमा (होली)", "2083-11-27", 2083, 11, 27, true, "GOVERNMENT"));

        // Chaitra (Month 12)
        seeds.add(new CalendarEvent("घोडे जात्रा", "2083-12-15", 2083, 12, 15, true, "GOVERNMENT"));
        seeds.add(new CalendarEvent("रामनवमी", "2083-12-24", 2083, 12, 24, true, "GOVERNMENT"));

        for (CalendarEvent seed : seeds) {
            if (!calendarEventRepository.existsByBsDateAndTitle(seed.getBsDate(), seed.getTitle())) {
                calendarEventRepository.save(seed);
            }
        }
    }
}
