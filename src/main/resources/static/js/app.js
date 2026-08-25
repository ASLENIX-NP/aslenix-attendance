/* ============================================================
   ASLENIX ATTENDANCE SCANNER
============================================================ */

console.log("ASLENIX attendance scanner app.js loaded.");


/* ============================================================
   CONFIGURATION
============================================================ */

const ATTENDANCE_ENDPOINT =
    "/employee/attendance/qr";

const SCANNER_ELEMENT =
    "reader";

const QR_SCAN_COOLDOWN =
    4000;


/* ============================================================
   GLOBAL VARIABLES
============================================================ */

let html5QrCode = null;

let cameraRunning = false;

let processingScan = false;

let lastQrToken = null;

let lastScanTime = 0;


/* ============================================================
   DOM READY
============================================================ */

document.addEventListener(
    "DOMContentLoaded",
    function () {

        console.log(
            "HTML5Qrcode available:",
            typeof Html5Qrcode !== "undefined"
        );

        console.log(
            "Attendance endpoint:",
            ATTENDANCE_ENDPOINT
        );


        initializeClock();

        initializeScanner();

        initializeLogout();

        initializeEmployeeInformation();

    }
);


/* ============================================================
   EMPLOYEE INFORMATION
============================================================ */

function initializeEmployeeInformation() {

    const employee =
        document.getElementById(
            "todayEmployee"
        );

    if (!employee) {
        return;
    }

    const name =
        employee.dataset.employeeName || "Employee";

    const code =
        employee.dataset.employeeCode || "—";

    const department =
        employee.dataset.employeeDepartment || "—";


    const resultName =
        document.getElementById(
            "employeeName"
        );

    const resultCode =
        document.getElementById(
            "employeeCode"
        );

    const resultDepartment =
        document.getElementById(
            "employeeDepartment"
        );


    if (resultName) {

        resultName.textContent =
            name;
    }


    if (resultCode) {

        resultCode.textContent =
            code;
    }


    if (resultDepartment) {

        resultDepartment.textContent =
            department;
    }

}


/* ============================================================
   CLOCK
============================================================ */

function initializeClock() {

    updateClock();

    setInterval(
        updateClock,
        1000
    );

}


function updateClock() {

    const clock =
        document.getElementById(
            "clock"
        );

    const zone =
        document.getElementById(
            "zone"
        );


    if (!clock) {
        return;
    }


    const now =
        new Date();


    clock.textContent =
        now.toLocaleTimeString(
            "en-US",
            {
                hour: "2-digit",
                minute: "2-digit",
                second: "2-digit"
            }
        );


    if (zone) {

        zone.textContent =
            Intl.DateTimeFormat()
                .resolvedOptions()
                .timeZone;
    }

}


/* ============================================================
   SCANNER INITIALIZATION
============================================================ */

function initializeScanner() {

    const startButton =
        document.getElementById(
            "start"
        );

    const stopButton =
        document.getElementById(
            "stop"
        );


    if (!startButton || !stopButton) {

        console.error(
            "Scanner buttons not found."
        );

        return;
    }


    startButton.addEventListener(
        "click",
        startCamera
    );


    stopButton.addEventListener(
        "click",
        stopCamera
    );


    setCameraStatus(
        false
    );

}


/* ============================================================
   START CAMERA
============================================================ */

async function startCamera() {

    console.log(
        "Starting QR camera..."
    );


    if (
        typeof Html5Qrcode ===
        "undefined"
    ) {

        showNotice(
            "QR scanner library failed to load. Please refresh the page.",
            "error"
        );

        console.error(
            "Html5Qrcode is not available."
        );

        return;
    }


    if (cameraRunning) {

        console.log(
            "Camera already running."
        );

        return;
    }


    try {

        /*
         * Request location permission before scanning.
         */

        await requestLocation();


        html5QrCode =
            new Html5Qrcode(
                SCANNER_ELEMENT
            );


        /*
         * Use a STRING for facingMode.
         *
         * This avoids:
         *
         * "'facingMode' should be string or object with exact as key"
         */

        const cameraConfig = {
            facingMode: "environment"
        };


        const qrConfig = {

            fps: 10,

            qrbox: function (
                viewfinderWidth,
                viewfinderHeight
            ) {

                const minEdge =
                    Math.min(
                        viewfinderWidth,
                        viewfinderHeight
                    );

                const size =
                    Math.floor(
                        minEdge * 0.65
                    );

                return {
                    width: size,
                    height: size
                };

            },

            aspectRatio: 1.0,

            disableFlip: false

        };


        await html5QrCode.start(

            cameraConfig,

            qrConfig,

            onQrDetected,

            onQrScanError

        );


        cameraRunning = true;

        processingScan = false;

        lastQrToken = null;

        lastScanTime = 0;


        setCameraStatus(
            true
        );


        const placeholder =
            document.getElementById(
                "placeholder"
            );


        if (placeholder) {

            placeholder.style.display =
                "none";
        }


        const startButton =
            document.getElementById(
                "start"
            );

        const stopButton =
            document.getElementById(
                "stop"
            );


        if (startButton) {

            startButton.disabled =
                true;
        }


        if (stopButton) {

            stopButton.disabled =
                false;
        }


        showNotice(
            "Camera started. Point it at your employee QR code.",
            "info"
        );


        console.log(
            "Camera started successfully."
        );

    } catch (error) {

        console.error(
            "Camera start error:",
            error
        );


        cameraRunning = false;


        setCameraStatus(
            false
        );


        showNotice(
            getCameraErrorMessage(
                error
            ),
            "error"
        );

    }

}


/* ============================================================
   CAMERA ERROR MESSAGE
============================================================ */

function getCameraErrorMessage(
    error
) {

    if (!error) {

        return (
            "Unable to start the camera."
        );
    }


    const message =
        String(error.message || error);


    if (
        message.includes(
            "Permission denied"
        )
        ||
        message.includes(
            "NotAllowedError"
        )
    ) {

        return (
            "Camera permission was denied. " +
            "Allow camera access in your browser settings."
        );
    }


    if (
        message.includes(
            "NotFoundError"
        )
        ||
        message.includes(
            "DevicesNotFoundError"
        )
    ) {

        return (
            "No camera was found on this device."
        );
    }


    return (
        "Unable to start the camera: " +
        message
    );

}


/* ============================================================
   STOP CAMERA
============================================================ */

async function stopCamera() {

    console.log(
        "Stopping QR camera..."
    );


    if (!html5QrCode) {

        setCameraStatus(
            false
        );

        return;
    }


    try {

        if (cameraRunning) {

            await html5QrCode.stop();

            console.log(
                "Camera stopped."
            );
        }

    } catch (error) {

        console.error(
            "Camera stop error:",
            error
        );

    } finally {

        cameraRunning = false;

        processingScan = false;

        html5QrCode = null;

        setCameraStatus(
            false
        );


        const placeholder =
            document.getElementById(
                "placeholder"
            );


        if (placeholder) {

            placeholder.style.display =
                "flex";
        }


        const startButton =
            document.getElementById(
                "start"
            );

        const stopButton =
            document.getElementById(
                "stop"
            );


        if (startButton) {

            startButton.disabled =
                false;
        }


        if (stopButton) {

            stopButton.disabled =
                true;
        }

    }

}


/* ============================================================
   CAMERA STATUS
============================================================ */

function setCameraStatus(
    active
) {

    const badge =
        document.getElementById(
            "badge"
        );


    if (!badge) {
        return;
    }


    if (active) {

        badge.textContent =
            "Camera Active";

        badge.classList.add(
            "active"
        );

    } else {

        badge.textContent =
            "Camera Off";

        badge.classList.remove(
            "active"
        );

    }

}


/* ============================================================
   QR DETECTED
============================================================ */

function onQrDetected(
    decodedText,
    decodedResult
) {

    if (!decodedText) {
        return;
    }


    const qrToken =
        decodedText.trim();


    console.log(
        "QR DETECTED:",
        qrToken
    );


    /*
     * Prevent multiple detections
     * of the same QR frame.
     */

    const now =
        Date.now();


    if (
        processingScan
    ) {

        console.log(
            "Scan already processing."
        );

        return;
    }


    if (
        lastQrToken === qrToken
        &&
        now - lastScanTime <
            QR_SCAN_COOLDOWN
    ) {

        console.log(
            "Duplicate QR ignored."
        );

        return;
    }


    lastQrToken =
        qrToken;

    lastScanTime =
        now;


    processQrAttendance(
        qrToken
    );

}


/* ============================================================
   QR SCAN ERROR
============================================================ */

function onQrScanError(
    errorMessage
) {

    /*
     * html5-qrcode calls this constantly
     * while looking for a QR.
     *
     * Do not display every scan error
     * to the user.
     */

}


/* ============================================================
   PROCESS QR ATTENDANCE
============================================================ */

async function processQrAttendance(
    qrToken
) {

    console.log(
        "QR TOKEN:",
        qrToken
    );


    processingScan =
        true;


    showNotice(
        "QR code detected. Checking your location...",
        "info"
    );


    try {

        const location =
            await requestLocation();


        console.log(
            "GPS:",
            location
        );


        const response =
            await sendAttendanceRequest(
                qrToken,
                location.latitude,
                location.longitude
            );


        console.log(
            "Attendance response:",
            response
        );


        if (
            response &&
            response.success
        ) {

            handleSuccessfulAttendance(
                response
            );

        } else {

            handleAttendanceError(
                response
            );

        }

    } catch (error) {

        console.error(
            "Attendance processing error:",
            error
        );


        showNotice(
            error.message ||
            "Unable to record attendance.",
            "error"
        );

    } finally {

        /*
         * Give the camera a small cooldown.
         */

        setTimeout(
            function () {

                processingScan =
                    false;

            },
            1500
        );

    }

}


/* ============================================================
   REQUEST LOCATION
============================================================ */

function requestLocation() {

    return new Promise(
        function (
            resolve,
            reject
        ) {

            if (
                !navigator.geolocation
            ) {

                reject(
                    new Error(
                        "Geolocation is not supported by this browser."
                    )
                );

                return;
            }


            navigator.geolocation.getCurrentPosition(

                function (position) {

                    const latitude =
                        position.coords.latitude;

                    const longitude =
                        position.coords.longitude;


                    console.log(
                        "Latitude:",
                        latitude
                    );

                    console.log(
                        "Longitude:",
                        longitude
                    );

                    console.log(
                        "GPS accuracy:",
                        position.coords.accuracy,
                        "meters"
                    );


                    resolve({

                        latitude:
                            latitude,

                        longitude:
                            longitude,

                        accuracy:
                            position.coords.accuracy

                    });

                },

                function (error) {

                    console.error(
                        "Location error:",
                        error
                    );


                    let message =
                        "Unable to get your location.";


                    if (
                        error.code ===
                        error.PERMISSION_DENIED
                    ) {

                        message =
                            "Location permission was denied. Please allow location access and try again.";

                    } else if (
                        error.code ===
                        error.POSITION_UNAVAILABLE
                    ) {

                        message =
                            "Your location is currently unavailable.";

                    } else if (
                        error.code ===
                        error.TIMEOUT
                    ) {

                        message =
                            "Location request timed out. Please try again.";
                    }


                    reject(
                        new Error(
                            message
                        )
                    );

                },

                {

                    enableHighAccuracy:
                        true,

                    timeout:
                        15000,

                    maximumAge:
                        0

                }

            );

        }
    );

}


/* ============================================================
   SEND ATTENDANCE REQUEST
============================================================ */

async function sendAttendanceRequest(
    qrToken,
    latitude,
    longitude
) {

    const formData =
        new URLSearchParams();


    formData.append(
        "qrToken",
        qrToken
    );


    formData.append(
        "latitude",
        latitude
    );


    formData.append(
        "longitude",
        longitude
    );


    /*
     * CSRF
     */

    const csrfTokenElement =
        document.querySelector(
            'meta[name="_csrf"]'
        );

    const csrfHeaderElement =
        document.querySelector(
            'meta[name="_csrf_header"]'
        );


    const headers = {

        "Content-Type":
            "application/x-www-form-urlencoded",

        "Accept":
            "application/json"

    };


    if (
        csrfTokenElement
        &&
        csrfHeaderElement
    ) {

        headers[
            csrfHeaderElement.content
        ] =
            csrfTokenElement.content;


        console.log(
            "CSRF header added."
        );

    } else {

        console.warn(
            "CSRF meta tags not found."
        );

    }


    console.log(
        "Sending attendance request..."
    );

    console.log(
        "Endpoint:",
        ATTENDANCE_ENDPOINT
    );


    const response =
        await fetch(
            ATTENDANCE_ENDPOINT,
            {

                method: "POST",

                headers: headers,

                body: formData,

                credentials:
                    "same-origin"

            }
        );


    console.log(
        "HTTP STATUS:",
        response.status
    );


    const contentType =
        response.headers.get(
            "content-type"
        ) || "";


    let data;


    if (
        contentType.includes(
            "application/json"
        )
    ) {

        data =
            await response.json();

    } else {

        const text =
            await response.text();


        console.error(
            "SERVER RESPONSE TEXT:",
            text
        );


        throw new Error(
            "Server returned an unexpected response. HTTP status: " +
            response.status
        );

    }


    console.log(
        "SERVER RESPONSE:",
        data
    );


    if (
        !response.ok
    ) {

        const error =
            new Error(
                data.message ||
                "Attendance request failed."
            );


        error.status =
            response.status;

        error.data =
            data;


        throw error;
    }


    return data;

}


/* ============================================================
   SUCCESSFUL ATTENDANCE
============================================================ */

function handleSuccessfulAttendance(
    data
) {

    console.log(
        "SUCCESSFUL ATTENDANCE:",
        data
    );


    /*
     * Show success message.
     */

    showNotice(
        data.message ||
        "Attendance recorded successfully.",
        "success"
    );


    /*
     * Update attendance result.
     */

    updateAttendanceResult(
        data
    );


    /*
     * Update Today's Attendance.
     */

    updateTodayAttendance(
        data
    );


    /*
     * Stop scanner after successful
     * attendance.
     */

    stopCamera();


    /*
     * Make the result visible.
     */

    const result =
        document.getElementById(
            "scanResult"
        );


    if (result) {

        result.style.display =
            "block";


        setTimeout(
            function () {

                result.scrollIntoView({

                    behavior:
                        "smooth",

                    block:
                        "center"

                });

            },
            100
        );

    }

}


/* ============================================================
   ATTENDANCE ERROR
============================================================ */

function handleAttendanceError(
    data
) {

    console.error(
        "Attendance failed:",
        data
    );


    const message =
        data &&
        data.message
            ? data.message
            : "Attendance could not be recorded.";


    showNotice(
        message,
        "error"
    );


    /*
     * Do not stop camera for normal
     * attendance validation errors.
     *
     * This allows the employee to try again.
     */

}


/* ============================================================
   UPDATE ATTENDANCE RESULT
============================================================ */

function updateAttendanceResult(
    data
) {

    const result =
        document.getElementById(
            "scanResult"
        );


    const employeeName =
        document.getElementById(
            "employeeName"
        );


    const employeeCode =
        document.getElementById(
            "employeeCode"
        );


    const employeeDepartment =
        document.getElementById(
            "employeeDepartment"
        );


    const scanAction =
        document.getElementById(
            "scanAction"
        );


    const scanTime =
        document.getElementById(
            "scanTime"
        );


    const scanDistance =
        document.getElementById(
            "scanDistance"
        );


    /*
     * Get employee information from
     * the page.
     */

    const todayEmployee =
        document.getElementById(
            "todayEmployee"
        );


    if (todayEmployee) {

        const name =
            todayEmployee.dataset.employeeName
            ||
            todayEmployee.textContent.trim()
            ||
            "Employee";


        const code =
            todayEmployee.dataset.employeeCode
            ||
            "—";


        const department =
            todayEmployee.dataset.employeeDepartment
            ||
            "—";


        if (employeeName) {

            employeeName.textContent =
                name;
        }


        if (employeeCode) {

            employeeCode.textContent =
                code;
        }


        if (employeeDepartment) {

            employeeDepartment.textContent =
                department;
        }

    }


    /*
     * Attendance action
     */

    if (scanAction) {

        if (
            data.action ===
            "CHECK_IN"
        ) {

            scanAction.textContent =
                "CHECK IN";

        } else if (
            data.action ===
            "CHECK_OUT"
        ) {

            scanAction.textContent =
                "CHECK OUT";

        } else {

            scanAction.textContent =
                data.action ||
                "—";
        }

    }


    /*
     * Time
     */

    if (scanTime) {

        scanTime.textContent =
            formatAttendanceTime(
                data.time
            );

    }


    /*
     * Distance
     */

    if (scanDistance) {

        if (
            data.distance !==
            undefined
            &&
            data.distance !== null
        ) {

            scanDistance.textContent =
                Math.round(
                    Number(
                        data.distance
                    )
                )
                + " m";

        } else {

            scanDistance.textContent =
                "—";
        }

    }


    /*
     * Display result card.
     */

    if (result) {

        result.style.display =
            "block";

        result.style.borderLeftColor =
            "#16a34a";
    }

}


/* ============================================================
   UPDATE TODAY'S ATTENDANCE
============================================================ */

function updateTodayAttendance(
    data
) {

    const checkIn =
        document.getElementById(
            "todayCheckIn"
        );


    const checkOut =
        document.getElementById(
            "todayCheckOut"
        );


    const status =
        document.getElementById(
            "todayStatus"
        );


    /*
     * CHECK IN
     */

    if (
        data.action ===
        "CHECK_IN"
    ) {

        if (checkIn) {

            checkIn.textContent =
                formatAttendanceTime(
                    data.time
                );
        }


        if (checkOut) {

            checkOut.textContent =
                "--:--";
        }


        if (status) {

            status.textContent =
                "Working";

            status.className =
                "status status-working";
        }

    }


    /*
     * CHECK OUT
     */

    if (
        data.action ===
        "CHECK_OUT"
    ) {

        if (checkOut) {

            checkOut.textContent =
                formatAttendanceTime(
                    data.time
                );
        }


        if (status) {

            status.textContent =
                "Completed";

            status.className =
                "status status-completed";
        }

    }

}


/* ============================================================
   FORMAT TIME
============================================================ */

function formatAttendanceTime(
    time
) {

    if (!time) {

        return "—";
    }


    const parts =
        String(time).split(
            ":"
        );


    if (
        parts.length <
        2
    ) {

        return time;
    }


    let hour =
        parseInt(
            parts[0],
            10
        );


    const minute =
        parts[1];


    const period =
        hour >= 12
            ? "PM"
            : "AM";


    hour =
        hour % 12 || 12;


    return (
        hour +
        ":" +
        minute +
        " " +
        period
    );

}


/* ============================================================
   SHOW NOTICE
============================================================ */

function showNotice(
    message,
    type
) {

    const notice =
        document.getElementById(
            "notice"
        );


    if (!notice) {
        return;
    }


    notice.textContent =
        message ||
        "";


    notice.className =
        "message " +
        (
            type ||
            "info"
        );


    notice.style.display =
        "block";


    /*
     * Automatically hide info messages.
     * Keep success/error messages visible
     * a little longer.
     */

    const timeout =
        type === "error"
            ? 7000
            : type === "success"
                ? 6000
                : 5000;


    setTimeout(
        function () {

            if (
                notice.textContent ===
                message
            ) {

                notice.style.display =
                    "none";
            }

        },
        timeout
    );

}


/* ============================================================
   LOGOUT
============================================================ */

function initializeLogout() {

    const logoutButton =
        document.getElementById(
            "logout"
        );


    if (!logoutButton) {
        return;
    }


    logoutButton.addEventListener(
        "click",
        async function () {

            try {

                /*
                 * Stop camera first.
                 */

                await stopCamera();


                /*
                 * Get CSRF.
                 */

                const csrfTokenElement =
                    document.querySelector(
                        'meta[name="_csrf"]'
                    );

                const csrfHeaderElement =
                    document.querySelector(
                        'meta[name="_csrf_header"]'
                    );


                const headers = {};


                if (
                    csrfTokenElement
                    &&
                    csrfHeaderElement
                ) {

                    headers[
                        csrfHeaderElement.content
                    ] =
                        csrfTokenElement.content;
                }


                /*
                 * Spring Security logout.
                 */

                const response =
                    await fetch(
                        "/logout",
                        {

                            method:
                                "POST",

                            headers:
                                headers,

                            credentials:
                                "same-origin"

                        }
                    );


                /*
                 * Spring Security normally
                 * redirects after logout.
                 */

                if (
                    response.redirected
                ) {

                    window.location.href =
                        response.url;

                } else {

                    window.location.href =
                        "/login";

                }

            } catch (error) {

                console.error(
                    "Logout error:",
                    error
                );


                window.location.href =
                    "/login";
            }

        }
    );

}