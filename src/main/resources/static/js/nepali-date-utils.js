/* =========================================================
   ASLENIX NEPALI BIKRAM SAMBAT (BS) DATE UTILITIES
========================================================= */

(function () {
    'use strict';

    const NEPALI_MONTHS_EN = [
        'Baisakh', 'Jestha', 'Ashadh', 'Shrawan',
        'Bhadra', 'Ashwin', 'Kartik', 'Mangsir',
        'Poush', 'Magh', 'Falgun', 'Chaitra'
    ];

    const NEPALI_MONTHS_SHORT = [
        'Bai', 'Jes', 'Ash', 'Shr',
        'Bha', 'Ash', 'Kar', 'Man',
        'Pou', 'Mag', 'Fal', 'Cha'
    ];

    const DAY_NAMES_EN = [
        'Sunday', 'Monday', 'Tuesday',
        'Wednesday', 'Thursday', 'Friday', 'Saturday'
    ];

    const DAY_NAMES_SHORT = [
        'Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'
    ];

    /* ---------------------------------------------
       CONVERT NEPALI DIGITS (०-९) TO ENGLISH (0-9)
    --------------------------------------------- */
    function convertNepaliNumbersToEnglish(value) {
        if (!value) return '';
        const nepaliNumbers = '०१२३४५६७८९';
        const englishNumbers = '0123456789';
        return String(value).replace(/[०-९]/g, function (ch) {
            const idx = nepaliNumbers.indexOf(ch);
            return idx >= 0 ? englishNumbers[idx] : ch;
        });
    }

    /* ---------------------------------------------
       PARSE AD DATE STRING (YYYY-MM-DD or ISO)
    --------------------------------------------- */
    function parseAdDate(str) {
        if (!str) return null;
        str = String(str).trim();
        const datePart = str.includes('T') ? str.split('T')[0] : str.split(' ')[0];
        const parts = datePart.split('-');
        if (parts.length < 3) return null;
        const year = parseInt(parts[0], 10);
        const month = parseInt(parts[1], 10) - 1; // 0-indexed in JS
        const day = parseInt(parts[2], 10);
        if (isNaN(year) || isNaN(month) || isNaN(day)) return null;
        return new Date(year, month, day);
    }

    function getNepaliDateClass() {
        try {
            if (typeof NepaliDate === 'function') return NepaliDate;
            if (typeof NepaliDate === 'object' && NepaliDate && typeof NepaliDate.default === 'function') return NepaliDate.default;
            if (typeof window !== 'undefined') {
                if (typeof window.NepaliDate === 'function') return window.NepaliDate;
                if (typeof window.NepaliDate === 'object' && window.NepaliDate && typeof window.NepaliDate.default === 'function') return window.NepaliDate.default;
            }
        } catch (e) {}
        return null;
    }

    /* ---------------------------------------------
       DETERMINISTIC BS DATE CALCULATOR (ANCHOR-BASED)
    --------------------------------------------- */
    function calculateBsDate(jsDate) {
        if (!jsDate || isNaN(jsDate.getTime())) return null;
        const y = jsDate.getFullYear();
        const m = jsDate.getMonth() + 1;
        const d = jsDate.getDate();

        // 2026-04-14 AD was 2083-01-01 BS (Baisakh 1, 2083)
        const anchorAd = new Date(2026, 3, 14);
        const curDate = new Date(y, m - 1, d);
        const diffDays = Math.round((curDate - anchorAd) / (1000 * 60 * 60 * 24));

        if (diffDays >= 0 && diffDays <= 365) {
            const lengths2083 = [31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30];
            let rem = diffDays;
            let bsM = 0;
            while (bsM < 12 && rem >= lengths2083[bsM]) {
                rem -= lengths2083[bsM];
                bsM++;
            }
            return {
                year: 2083,
                month: bsM + 1,
                day: rem + 1
            };
        }

        // 2025-04-14 AD was 2082-01-01 BS (Baisakh 1, 2082)
        const anchor2082 = new Date(2025, 3, 14);
        const diff2082 = Math.round((curDate - anchor2082) / (1000 * 60 * 60 * 24));
        if (diff2082 >= 0 && diff2082 <= 365) {
            const lengths2082 = [31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30];
            let rem = diff2082;
            let bsM = 0;
            while (bsM < 12 && rem >= lengths2082[bsM]) {
                rem -= lengths2082[bsM];
                bsM++;
            }
            return {
                year: 2082,
                month: bsM + 1,
                day: rem + 1
            };
        }

        // General fallback
        return {
            year: y + 57,
            month: m,
            day: d
        };
    }

    /* ---------------------------------------------
       FORMAT AD DATE TO BS STRING
    --------------------------------------------- */
    function convertAdToBsString(jsDate, format) {
        if (!jsDate || isNaN(jsDate.getTime())) return '';
        format = format || 'DD MMM YYYY';

        const NepaliDateClass = getNepaliDateClass();
        if (NepaliDateClass) {
            try {
                const nepaliDate = new NepaliDateClass(jsDate);
                return nepaliDate.format(format);
            } catch (e) {
                // proceed to fallback
            }
        }

        try {
            if (typeof NepaliFunctions !== 'undefined' && NepaliFunctions.AD2BS) {
                const bsObj = NepaliFunctions.AD2BS({
                    year: jsDate.getFullYear(),
                    month: jsDate.getMonth() + 1,
                    day: jsDate.getDate()
                });
                if (bsObj) {
                    return formatBsObject(bsObj, jsDate, format);
                }
            }
        } catch (e) {
            console.error('BS conversion error:', e);
        }

        const calc = calculateBsDate(jsDate);
        if (calc) {
            return formatBsObject(calc, jsDate, format);
        }

        return jsDate.toISOString().split('T')[0];
    }

    function formatBsObject(bsObj, jsDate, format) {
        const mIdx = Math.max(0, Math.min(11, bsObj.month - 1));
        const mName = NEPALI_MONTHS_EN[mIdx] || ('Month ' + bsObj.month);
        const mShort = NEPALI_MONTHS_SHORT[mIdx] || mName;
        const dayStr = String(bsObj.day).padStart(2, '0');
        const dName = DAY_NAMES_EN[jsDate.getDay()];
        const dShort = DAY_NAMES_SHORT[jsDate.getDay()];

        let res = format;
        res = res.replace(/dddd/g, dName);
        res = res.replace(/ddd/g, dShort);
        res = res.replace(/DD/g, dayStr);
        res = res.replace(/D/g, String(bsObj.day));
        res = res.replace(/MMMM/g, mName);
        res = res.replace(/MMM/g, mShort);
        res = res.replace(/MM/g, String(bsObj.month).padStart(2, '0'));
        res = res.replace(/M/g, String(bsObj.month));
        res = res.replace(/YYYY/g, String(bsObj.year));
        res = res.replace(/YY/g, String(bsObj.year).slice(-2));
        return res;
    }

    /* ---------------------------------------------
       FORMAT ALL [data-ad-date] & .bs-date ELEMENTS
    --------------------------------------------- */
    function formatAllBsDates() {
        // Elements with data-ad-date
        document.querySelectorAll('[data-ad-date]').forEach(function (el) {
            const adStr = el.getAttribute('data-ad-date');
            if (!adStr) return;
            const jsDate = parseAdDate(adStr);
            if (!jsDate) return;
            const format = el.getAttribute('data-bs-format') || 'DD MMM YYYY';
            const prefix = el.getAttribute('data-bs-prefix') || '';
            const suffix = el.getAttribute('data-bs-suffix') || '';
            const bsStr = convertAdToBsString(jsDate, format);
            if (bsStr) {
                el.textContent = prefix + bsStr + suffix;
            }
        });

        // Elements with data-ad-start and data-ad-end (Date Ranges)
        document.querySelectorAll('[data-ad-start]').forEach(function (el) {
            const startStr = el.getAttribute('data-ad-start');
            const endStr = el.getAttribute('data-ad-end');
            if (!startStr) return;
            const startDate = parseAdDate(startStr);
            const endDate = endStr ? parseAdDate(endStr) : null;
            if (!startDate) return;

            const startBs = convertAdToBsString(startDate, 'DD MMM');
            if (endDate) {
                const endBs = convertAdToBsString(endDate, 'DD MMM YYYY');
                el.textContent = startBs + ' – ' + endBs + ' BS';
            } else {
                el.textContent = convertAdToBsString(startDate, 'DD MMM YYYY') + ' BS';
            }
        });

        // Elements with class .bs-date-text that have raw date text (YYYY-MM-DD)
        document.querySelectorAll('.bs-date-text').forEach(function (el) {
            if (el.dataset.adDate) return; // already handled
            const rawText = el.textContent.trim();
            const jsDate = parseAdDate(rawText);
            if (jsDate) {
                const format = el.getAttribute('data-bs-format') || 'DD MMM YYYY';
                const prefix = el.getAttribute('data-bs-prefix') || '';
                const bsStr = convertAdToBsString(jsDate, format);
                if (bsStr) {
                    el.textContent = prefix + bsStr;
                }
            }
        });
    }

    /* ---------------------------------------------
       FORMAT LIVE HEADER BS DATE
    --------------------------------------------- */
    function formatHeaderBsDate() {
        const headerEl = document.getElementById('headerBsDate');
        if (!headerEl) return;

        try {
            const now = new Date();
            const dayOfWeek = DAY_NAMES_EN[now.getDay()];
            const NepaliDateClass = getNepaliDateClass();

            if (NepaliDateClass) {
                try {
                    const nepaliDate = new NepaliDateClass(now);
                    const year = nepaliDate.getYear();
                    const month = NEPALI_MONTHS_EN[nepaliDate.getMonth()];
                    const day = nepaliDate.getDate();
                    headerEl.textContent = `${dayOfWeek}, ${month} ${day}, ${year} BS`;
                    return;
                } catch (e) {}
            }

            if (typeof NepaliFunctions !== 'undefined' && NepaliFunctions.AD2BS) {
                try {
                    const bsObj = NepaliFunctions.AD2BS({
                        year: now.getFullYear(),
                        month: now.getMonth() + 1,
                        day: now.getDate()
                    });
                    if (bsObj) {
                        const month = NEPALI_MONTHS_EN[bsObj.month - 1];
                        headerEl.textContent = `${dayOfWeek}, ${month} ${bsObj.day}, ${bsObj.year} BS`;
                        return;
                    }
                } catch (e) {}
            }

            const calc = calculateBsDate(now);
            if (calc) {
                const month = NEPALI_MONTHS_EN[calc.month - 1];
                headerEl.textContent = `${dayOfWeek}, ${month} ${calc.day}, ${calc.year} BS`;
                return;
            }
        } catch (err) {
            console.error('Header BS date format error:', err);
        }
    }

    /* ---------------------------------------------
       INITIALIZE NEPALI DATE PICKERS (.use-nepali-datepicker)
    --------------------------------------------- */
    function initNepaliDatePickers() {
        const pickers = document.querySelectorAll('.use-nepali-datepicker');

        pickers.forEach(function (adInput) {
            if (adInput.dataset.nepaliInitialized === 'true') return;
            adInput.dataset.nepaliInitialized = 'true';

            // 1. Hide the original AD input
            adInput.style.display = 'none';

            // 2. Create the visible BS text input
            const bsInput = document.createElement('input');
            bsInput.type = 'text';
            // Inherit classes except use-nepali-datepicker
            bsInput.className = (adInput.className.replace('use-nepali-datepicker', '') + ' nepali-datepicker-input').trim();
            bsInput.placeholder = adInput.placeholder || 'Select Nepali Date (BS)';
            bsInput.autocomplete = 'off';
            if (adInput.required) bsInput.required = true;
            if (adInput.disabled) bsInput.disabled = true;

            // 3. Insert BS input before AD input
            adInput.parentNode.insertBefore(bsInput, adInput);

            // 4. Pre-fill BS input if AD input has an initial value
            if (adInput.value && adInput.value.trim()) {
                const parts = adInput.value.trim().split('-');
                if (parts.length === 3 && typeof NepaliFunctions !== 'undefined' && NepaliFunctions.AD2BS) {
                    try {
                        const bsObj = NepaliFunctions.AD2BS({
                            year: parseInt(parts[0], 10),
                            month: parseInt(parts[1], 10),
                            day: parseInt(parts[2], 10)
                        });
                        if (bsObj) {
                            bsInput.value = bsObj.year + '-' +
                                           String(bsObj.month).padStart(2, '0') + '-' +
                                           String(bsObj.day).padStart(2, '0');
                        }
                    } catch (e) {
                        console.error('AD to BS init error:', e);
                    }
                }
            }

            // 5. Initialize the Nepali Date Picker widget
            if (typeof bsInput.nepaliDatePicker === 'function') {
                bsInput.nepaliDatePicker({
                    ndpYear: true,
                    ndpMonth: true,
                    ndpYearCount: 100,
                    onChange: function () {
                        syncBsToAd(bsInput, adInput);
                    }
                });
            }

            // 6. Handle change and input events
            bsInput.addEventListener('change', function () {
                syncBsToAd(bsInput, adInput);
            });
            bsInput.addEventListener('input', function () {
                syncBsToAd(bsInput, adInput);
            });
        });
    }

    /* ---------------------------------------------
       SYNC BS INPUT VALUE TO AD INPUT
    --------------------------------------------- */
    function syncBsToAd(bsInput, adInput) {
        let bsVal = bsInput.value ? bsInput.value.trim() : '';
        if (!bsVal) {
            adInput.value = '';
            adInput.dispatchEvent(new Event('change', { bubbles: true }));
            adInput.dispatchEvent(new Event('input', { bubbles: true }));
            return;
        }

        bsVal = convertNepaliNumbersToEnglish(bsVal);
        bsInput.value = bsVal;

        if (typeof NepaliFunctions !== 'undefined' && NepaliFunctions.BS2AD) {
            try {
                const adObj = NepaliFunctions.BS2AD(bsVal);
                if (adObj) {
                    const y = String(adObj.year);
                    const m = String(adObj.month).padStart(2, '0');
                    const d = String(adObj.day).padStart(2, '0');
                    adInput.value = y + '-' + m + '-' + d;
                    adInput.dispatchEvent(new Event('change', { bubbles: true }));
                    adInput.dispatchEvent(new Event('input', { bubbles: true }));
                } else {
                    adInput.value = '';
                }
            } catch (err) {
                console.error('BS2AD conversion error:', err);
                adInput.value = '';
            }
        }
    }

    /* ---------------------------------------------
       GLOBAL EXPORTS
    --------------------------------------------- */
    window.formatAllBsDates = formatAllBsDates;
    window.formatHeaderBsDate = formatHeaderBsDate;
    window.initNepaliDatePickers = initNepaliDatePickers;
    window.convertAdToBsString = convertAdToBsString;
    window.convertNepaliNumbersToEnglish = convertNepaliNumbersToEnglish;

    /* ---------------------------------------------
       DOM READY LISTENER
    --------------------------------------------- */
    function init() {
        formatHeaderBsDate();
        formatAllBsDates();
        initNepaliDatePickers();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

})();
