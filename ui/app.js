const DEFAULT_RESULT_URL = "../examples/timetable.json";

const DAY_ORDER = ["mon", "tue", "wed", "thu", "fri", "sat", "sun"];
const DAY_NAMES = {
    mon: "Monday",
    tue: "Tuesday",
    wed: "Wednesday",
    thu: "Thursday",
    fri: "Friday",
    sat: "Saturday",
    sun: "Sunday"
};
const VIEW_LABELS = {
    cohortId: "Class",
    teacherId: "Teacher",
    roomId: "Room"
};
const SUBJECT_COLORS = [
    "#4776d0",
    "#a45fbb",
    "#d77b35",
    "#278b80",
    "#bf5369",
    "#7a69c6",
    "#548842",
    "#cb9931",
    "#497f9e",
    "#9b684a"
];

const elements = {
    fileInput: document.querySelector("#file-input"),
    schoolName: document.querySelector("#school-name"),
    resultState: document.querySelector("#result-state"),
    resultStatus: document.querySelector("#result-status"),
    lessonCount: document.querySelector("#lesson-count"),
    switcher: document.querySelector("#view-switcher"),
    entityLabel: document.querySelector("#entity-label"),
    entitySelect: document.querySelector("#entity-select"),
    message: document.querySelector("#message"),
    timetableWrap: document.querySelector("#timetable-wrap"),
    timetable: document.querySelector("#timetable"),
    mobileTimetable: document.querySelector("#mobile-timetable"),
    mobileDays: document.querySelector("#mobile-days"),
    mobileSchedule: document.querySelector("#mobile-schedule"),
    dropOverlay: document.querySelector("#drop-overlay")
};

const state = {
    result: null,
    assignments: [],
    view: "cohortId",
    selected: null,
    mobileDay: null
};

function humanize(value) {
    return String(value ?? "")
        .replace(/[-_.]+/g, " ")
        .replace(/\b\p{L}/gu, letter => letter.toLocaleUpperCase());
}

function displayEntity(value, key) {
    if (key === "roomId" || key === "cohortId") {
        return String(value).toLocaleUpperCase();
    }
    return humanize(value);
}

function parsePeriod(periodId) {
    const match = /^([a-z]+)[-_](\d+)$/i.exec(String(periodId));
    if (!match) {
        return { day: "other", slot: String(periodId), numericSlot: Number.MAX_SAFE_INTEGER };
    }
    return { day: match[1].toLocaleLowerCase(), slot: match[2], numericSlot: Number(match[2]) };
}

function validateResult(value) {
    if (!value || typeof value !== "object" || Array.isArray(value)) {
        throw new Error("The selected file does not contain a JSON object.");
    }
    if (!value.timetable || !Array.isArray(value.timetable.assignments)) {
        const status = value.status ? ` Its status is ${value.status}.` : "";
        throw new Error(`This result does not contain timetable.assignments.${status}`);
    }
    const required = ["lessonId", "subjectId", "cohortId", "teacherId", "periodId", "roomId"];
    const invalid = value.timetable.assignments.findIndex(assignment =>
        !assignment || required.some(key => typeof assignment[key] !== "string" || !assignment[key])
    );
    if (invalid >= 0) {
        throw new Error(`Assignment ${invalid + 1} is missing one or more required fields.`);
    }
    return value;
}

function showMessage(message) {
    elements.message.textContent = message;
    elements.message.hidden = false;
    elements.timetableWrap.hidden = true;
    elements.mobileTimetable.hidden = true;
}

function clearMessage() {
    elements.message.hidden = true;
    elements.timetableWrap.hidden = false;
    elements.mobileTimetable.hidden = false;
}

function setResult(result, sourceName) {
    state.result = validateResult(result);
    state.assignments = result.timetable.assignments;
    elements.schoolName.textContent = result.schoolId
        ? `${humanize(result.schoolId)} · ${sourceName}`
        : sourceName;
    elements.resultStatus.textContent = humanize(result.status || "Result");
    elements.lessonCount.textContent = `${state.assignments.length} ${state.assignments.length === 1 ? "lesson" : "lessons"}`;
    elements.resultState.hidden = false;
    clearMessage();
    setView(state.view);
}

function setView(view) {
    state.view = view;
    state.mobileDay = null;
    for (const button of elements.switcher.querySelectorAll("button")) {
        button.setAttribute("aria-pressed", String(button.dataset.view === view));
    }
    elements.entityLabel.textContent = VIEW_LABELS[view];

    const entities = [...new Set(state.assignments.map(assignment => assignment[view]))]
        .sort((left, right) => displayEntity(left, view).localeCompare(displayEntity(right, view), undefined, { numeric: true }));
    const previous = entities.includes(state.selected) ? state.selected : entities[0];
    state.selected = previous ?? null;

    elements.entitySelect.replaceChildren();
    for (const entity of entities) {
        const option = document.createElement("option");
        option.value = entity;
        option.textContent = displayEntity(entity, view);
        option.selected = entity === state.selected;
        elements.entitySelect.append(option);
    }
    elements.entitySelect.disabled = entities.length === 0;
    renderTimetable();
}

function subjectColor(subjectId) {
    let hash = 0;
    for (const character of subjectId) {
        hash = ((hash << 5) - hash + character.codePointAt(0)) | 0;
    }
    return SUBJECT_COLORS[Math.abs(hash) % SUBJECT_COLORS.length];
}

function lessonCard(assignment) {
    const article = document.createElement("article");
    article.className = "lesson";
    article.style.setProperty("--lesson-color", subjectColor(assignment.subjectId));
    article.title = assignment.lessonId;

    const subject = document.createElement("div");
    subject.className = "lesson-subject";
    subject.textContent = humanize(assignment.subjectId);

    const details = document.createElement("div");
    details.className = "lesson-details";
    const detailKeys = ["cohortId", "teacherId", "roomId"].filter(key => key !== state.view);
    for (const key of detailKeys) {
        const line = document.createElement("span");
        line.textContent = displayEntity(assignment[key], key);
        details.append(line);
    }

    article.append(subject, details);
    return article;
}

function mobileLessonCard(assignment, period) {
    const article = document.createElement("article");
    article.className = "mobile-lesson";
    article.style.setProperty("--lesson-color", subjectColor(assignment.subjectId));

    const periodBadge = document.createElement("div");
    periodBadge.className = "mobile-period";
    const periodLabel = document.createElement("span");
    periodLabel.textContent = "Lesson";
    const periodNumber = document.createElement("strong");
    periodNumber.textContent = period.slot;
    periodBadge.append(periodLabel, periodNumber);

    const content = document.createElement("div");
    content.className = "mobile-lesson-content";
    const subject = document.createElement("h3");
    subject.textContent = humanize(assignment.subjectId);
    const details = document.createElement("div");
    details.className = "mobile-lesson-details";
    const detailKeys = ["cohortId", "teacherId", "roomId"].filter(key => key !== state.view);
    for (const key of detailKeys) {
        const detail = document.createElement("span");
        detail.textContent = displayEntity(assignment[key], key);
        details.append(detail);
    }
    content.append(subject, details);
    article.append(periodBadge, content);
    return article;
}

function renderMobileTimetable(parsed, presentDays) {
    elements.mobileDays.replaceChildren();
    elements.mobileSchedule.replaceChildren();

    const lessonDays = new Set(parsed.map(item => item.period.day));
    if (!presentDays.includes(state.mobileDay)) {
        state.mobileDay = presentDays.find(day => lessonDays.has(day)) ?? presentDays[0] ?? null;
    }

    for (const day of presentDays) {
        const count = parsed.filter(item => item.period.day === day).length;
        const button = document.createElement("button");
        button.type = "button";
        button.id = `mobile-day-${day}`;
        button.setAttribute("role", "tab");
        button.dataset.day = day;
        button.setAttribute("aria-selected", String(day === state.mobileDay));
        button.setAttribute("aria-controls", "mobile-schedule");
        button.setAttribute("aria-label", `${DAY_NAMES[day] || humanize(day)}, ${count} ${count === 1 ? "lesson" : "lessons"}`);
        button.tabIndex = day === state.mobileDay ? 0 : -1;

        const name = document.createElement("span");
        name.className = "mobile-day-name";
        name.textContent = (DAY_NAMES[day] || humanize(day)).slice(0, 3);
        const badge = document.createElement("span");
        badge.className = "mobile-day-count";
        badge.textContent = count;
        button.append(name, badge);
        elements.mobileDays.append(button);
    }
    elements.mobileSchedule.setAttribute("aria-labelledby", `mobile-day-${state.mobileDay}`);

    const heading = document.createElement("div");
    heading.className = "mobile-schedule-heading";
    const title = document.createElement("h2");
    title.textContent = DAY_NAMES[state.mobileDay] || humanize(state.mobileDay);
    const dayAssignments = parsed
        .filter(item => item.period.day === state.mobileDay)
        .sort((left, right) => left.period.numericSlot - right.period.numericSlot
            || left.assignment.lessonId.localeCompare(right.assignment.lessonId));
    const summary = document.createElement("span");
    summary.textContent = `${dayAssignments.length} ${dayAssignments.length === 1 ? "lesson" : "lessons"}`;
    heading.append(title, summary);
    elements.mobileSchedule.append(heading);

    if (dayAssignments.length === 0) {
        const empty = document.createElement("p");
        empty.className = "mobile-empty";
        empty.textContent = "No lessons scheduled.";
        elements.mobileSchedule.append(empty);
        return;
    }

    const list = document.createElement("div");
    list.className = "mobile-lesson-list";
    for (const item of dayAssignments) {
        list.append(mobileLessonCard(item.assignment, item.period));
    }
    elements.mobileSchedule.append(list);
}

function renderTimetable() {
    elements.timetable.replaceChildren();
    if (!state.selected) {
        showMessage("This timetable has no assignments to display.");
        return;
    }
    clearMessage();

    const assignments = state.assignments.filter(assignment => assignment[state.view] === state.selected);
    const parsed = assignments.map(assignment => ({ assignment, period: parsePeriod(assignment.periodId) }));
    const calendarPeriods = state.assignments.map(assignment => parsePeriod(assignment.periodId));
    const presentDays = [...new Set(calendarPeriods.map(period => period.day))]
        .sort((left, right) => {
            const leftIndex = DAY_ORDER.indexOf(left);
            const rightIndex = DAY_ORDER.indexOf(right);
            return (leftIndex < 0 ? DAY_ORDER.length : leftIndex) - (rightIndex < 0 ? DAY_ORDER.length : rightIndex);
        });
    const slots = [...new Map(calendarPeriods
        .sort((left, right) => left.numericSlot - right.numericSlot)
        .map(period => [period.slot, period])).values()];

    const thead = document.createElement("thead");
    const headerRow = document.createElement("tr");
    const corner = document.createElement("th");
    corner.scope = "col";
    corner.textContent = "Day";
    headerRow.append(corner);
    for (const slot of slots) {
        const th = document.createElement("th");
        th.scope = "col";
        const number = document.createElement("span");
        number.className = "period-number";
        number.textContent = slot.slot;
        const caption = document.createElement("span");
        caption.className = "period-caption";
        caption.textContent = "Lesson";
        th.append(number, caption);
        headerRow.append(th);
    }
    thead.append(headerRow);

    const tbody = document.createElement("tbody");
    for (const day of presentDays) {
        const row = document.createElement("tr");
        const dayHeader = document.createElement("th");
        dayHeader.scope = "row";
        const dayName = document.createElement("span");
        dayName.className = "day-name";
        dayName.textContent = DAY_NAMES[day] || humanize(day);
        const count = parsed.filter(item => item.period.day === day).length;
        const dayCount = document.createElement("span");
        dayCount.className = "day-count";
        dayCount.textContent = `${count} ${count === 1 ? "lesson" : "lessons"}`;
        dayHeader.append(dayName, dayCount);
        row.append(dayHeader);

        for (const slot of slots) {
            const cell = document.createElement("td");
            const cellAssignments = parsed
                .filter(item => item.period.day === day && item.period.slot === slot.slot)
                .map(item => item.assignment)
                .sort((left, right) => left.lessonId.localeCompare(right.lessonId));
            for (const assignment of cellAssignments) {
                cell.append(lessonCard(assignment));
            }
            row.append(cell);
        }
        tbody.append(row);
    }

    elements.timetable.append(thead, tbody);
    renderMobileTimetable(parsed, presentDays);
}

async function loadFile(file) {
    if (!file) return;
    try {
        const text = await file.text();
        setResult(JSON.parse(text), file.name);
    } catch (error) {
        showMessage(error instanceof SyntaxError ? "The selected file is not valid JSON." : error.message);
    } finally {
        elements.fileInput.value = "";
    }
}

elements.switcher.addEventListener("click", event => {
    const button = event.target.closest("button[data-view]");
    if (button && state.result) setView(button.dataset.view);
});

elements.entitySelect.addEventListener("change", event => {
    state.selected = event.target.value;
    state.mobileDay = null;
    renderTimetable();
});

elements.mobileDays.addEventListener("click", event => {
    const button = event.target.closest("button[data-day]");
    if (!button) return;
    state.mobileDay = button.dataset.day;
    renderTimetable();
    elements.mobileDays.querySelector("button[aria-selected='true']")?.focus();
});

elements.mobileDays.addEventListener("keydown", event => {
    if (!["ArrowLeft", "ArrowRight", "Home", "End"].includes(event.key)) return;
    const buttons = [...elements.mobileDays.querySelectorAll("button[data-day]")];
    const currentIndex = buttons.indexOf(event.target.closest("button[data-day]"));
    if (currentIndex < 0) return;
    event.preventDefault();
    let nextIndex = currentIndex;
    if (event.key === "ArrowLeft") nextIndex = (currentIndex - 1 + buttons.length) % buttons.length;
    if (event.key === "ArrowRight") nextIndex = (currentIndex + 1) % buttons.length;
    if (event.key === "Home") nextIndex = 0;
    if (event.key === "End") nextIndex = buttons.length - 1;
    state.mobileDay = buttons[nextIndex].dataset.day;
    renderTimetable();
    elements.mobileDays.querySelector("button[aria-selected='true']")?.focus();
});

elements.fileInput.addEventListener("change", event => loadFile(event.target.files[0]));

let dragDepth = 0;
window.addEventListener("dragenter", event => {
    event.preventDefault();
    dragDepth += 1;
    elements.dropOverlay.classList.add("visible");
});
window.addEventListener("dragover", event => event.preventDefault());
window.addEventListener("dragleave", event => {
    event.preventDefault();
    dragDepth -= 1;
    if (dragDepth <= 0) {
        dragDepth = 0;
        elements.dropOverlay.classList.remove("visible");
    }
});
window.addEventListener("drop", event => {
    event.preventDefault();
    dragDepth = 0;
    elements.dropOverlay.classList.remove("visible");
    loadFile(event.dataTransfer.files[0]);
});

fetch(DEFAULT_RESULT_URL)
    .then(response => {
        if (!response.ok) throw new Error(`Example request failed with ${response.status}.`);
        return response.json();
    })
    .then(result => setResult(result, "examples/timetable.json"))
    .catch(() => showMessage("Open a timetable JSON to begin. The bundled example loads automatically when this page is served from the repository root."));
