/* =========================================================
   NEPALI DATE UTILITIES
========================================================= */

document.addEventListener('DOMContentLoaded', function () {

    /* ---------------------------------------------
       FORMAT AD DATES TO BS (DISPLAY ONLY)
    --------------------------------------------- */
    // Any element with data-ad-date="YYYY-MM-DD" will have its text replaced with BS date
    const dateElements = document.querySelectorAll('[data-ad-date]');

    dateElements.forEach(function (element) {
        const adDate = element.getAttribute('data-ad-date');
        if (!adDate) return;

        try {
            const parts = adDate.split('-');
            if (parts.length !== 3) return;

            const year = parseInt(parts[0], 10);
            const month = parseInt(parts[1], 10) - 1; // 0-indexed in JS
            const day = parseInt(parts[2], 10);

            if (typeof NepaliDate !== 'undefined') {
                const javascriptDate = new Date(year, month, day);
                const nepaliDate = new NepaliDate(javascriptDate);
                element.textContent = nepaliDate.format(element.dataset.bsFormat || 'DD MMM YYYY');
            } else if (typeof NepaliFunctions !== 'undefined') {
                const bsObj = NepaliFunctions.AD2BS({ year: year, month: month + 1, day: day });
                const monthName = NepaliFunctions.GetBsMonth(bsObj.month - 1);
                element.textContent = bsObj.day + ' ' + monthName + ' ' + bsObj.year;
            }
        } catch (error) {
            console.error('Error formatting date:', error);
            element.textContent = adDate;
        }
    });

    /* ---------------------------------------------
       FORMAT CURRENT BS DATE (HEADER)
    --------------------------------------------- */
    const headerDate = document.getElementById('headerBsDate');
    if (headerDate) {
        try {
            const now = new Date();
            if (typeof NepaliDate !== 'undefined') {
                const nepaliDate = new NepaliDate(now);
                headerDate.textContent = nepaliDate.format('DD MMM YYYY');
            } else if (typeof NepaliFunctions !== 'undefined') {
                const bsObj = NepaliFunctions.AD2BS({ year: now.getFullYear(), month: now.getMonth() + 1, day: now.getDate() });
                const monthName = NepaliFunctions.GetBsMonth(bsObj.month - 1);
                headerDate.textContent = bsObj.day + ' ' + monthName + ' ' + bsObj.year;
            }
        } catch (error) {
            headerDate.textContent = 'Attendance';
        }
    }

});

/* ---------------------------------------------
   CONVERT NEPALI NUMBERS TO ENGLISH
--------------------------------------------- */
function convertNepaliNumbersToEnglish(value) {
    if (!value) return value;
    const nepaliNumbers = "०१२३४५६७८९";
    const englishNumbers = "0123456789";
    return value.replace(/[०-९]/g, function (character) {
        const index = nepaliNumbers.indexOf(character);
        return index >= 0 ? englishNumbers[index] : character;
    });
}

/* ---------------------------------------------
   INITIALIZE NEPALI DATE PICKERS
--------------------------------------------- */
// Replaces actual AD date inputs with a visible BS input + hidden AD input
function initNepaliDatePickers() {
    const pickers = document.querySelectorAll('.use-nepali-datepicker');
    
    pickers.forEach(function (adInput) {
        if (adInput.dataset.initialized === 'true') return;
        adInput.dataset.initialized = 'true';

        // 1. Hide the original AD input
        adInput.style.display = 'none';
        
        // 2. Create the visible BS text input
        const bsInput = document.createElement('input');
        bsInput.type = 'text';
        bsInput.className = adInput.className.replace('use-nepali-datepicker', '') + ' nepali-datepicker';
        bsInput.placeholder = 'YYYY-MM-DD (BS)';
        if (adInput.required) bsInput.required = true;
        
        // 3. Insert BS input before AD input
        adInput.parentNode.insertBefore(bsInput, adInput);

        // 4. Pre-fill BS input if AD input has a value
        if (adInput.value) {
            const parts = adInput.value.split('-');
            if (parts.length === 3 && typeof NepaliFunctions !== 'undefined') {
                const bsObj = NepaliFunctions.AD2BS({
                    year: parseInt(parts[0], 10),
                    month: parseInt(parts[1], 10),
                    day: parseInt(parts[2], 10)
                });
                bsInput.value = bsObj.year + '-' + 
                               String(bsObj.month).padStart(2, '0') + '-' + 
                               String(bsObj.day).padStart(2, '0');
            }
        }

        // 5. Initialize the picker if available
        if (typeof bsInput.nepaliDatePicker !== 'undefined') {
            bsInput.nepaliDatePicker({
                ndpYear: true,
                ndpMonth: true,
                ndpYearCount: 100
            });
        }

        // 6. Handle change events to sync back to AD
        bsInput.addEventListener('change', function () {
            let bsDate = bsInput.value.trim();
            if (!bsDate) {
                adInput.value = '';
                return;
            }

            bsDate = convertNepaliNumbersToEnglish(bsDate);
            bsInput.value = bsDate;

            if (typeof NepaliFunctions !== 'undefined') {
                try {
                    const adObj = NepaliFunctions.BS2AD(bsDate);
                    if (adObj) {
                        adInput.value = String(adObj.year) + '-' + 
                                        String(adObj.month).padStart(2, '0') + '-' + 
                                        String(adObj.day).padStart(2, '0');
                        // Dispatch change event on the original input in case framework relies on it
                        adInput.dispatchEvent(new Event('change', { bubbles: true }));
                    } else {
                        adInput.value = '';
                    }
                } catch (e) {
                    console.error("BS to AD conversion error:", e);
                    adInput.value = '';
                }
            }
        });
    });
}

document.addEventListener('DOMContentLoaded', function () {
    if (typeof NepaliFunctions !== 'undefined') {
        initNepaliDatePickers();
    }
});
