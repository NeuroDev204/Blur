// Backend serializes some timestamps as Instant (with trailing "Z") and others as
// LocalDateTime (no timezone, e.g. "2026-06-10T14:00:00"). The server clock is UTC, so a
// timezone-less ISO string must be read as UTC — otherwise new Date() treats it as local
// time and shows a wrong offset (e.g. "7 hours ago" for a brand-new post in UTC+7).
export const parseTimestamp = (timestamp: string | number | Date): Date => {
    if (typeof timestamp === "string") {
        const isIsoDateTime = /^\d{4}-\d{2}-\d{2}[T ]\d{2}:\d{2}/.test(timestamp)
        const hasTimezone = /[zZ]$|[+-]\d{2}:?\d{2}$/.test(timestamp)
        if (isIsoDateTime && !hasTimezone) {
            return new Date(timestamp.replace(" ", "T") + "Z")
        }
    }
    return new Date(timestamp)
}

export const timeDifference = (timestamp: string | number | Date): string | undefined => {
    const date = parseTimestamp(timestamp)
    const diff = Date.now() - date.getTime()
    const seconds = Math.floor(diff / 1000)
    const minutes = Math.floor(seconds / 60)
    const hours = Math.floor(minutes / 60)
    const days = Math.floor(hours / 24)
    const weeks = Math.floor(days / 7)
    const months = Math.floor(weeks / 4)
    const years = Math.floor(months / 12)

    if (years > 0) {
        return years + " year" + (years === 1 ? "" : "s") + " ago"
    } else if (months > 0) {
        return months + " month" + (months === 1 ? "" : "s") + " ago"
    } else if (weeks > 0) {
        return weeks + " week" + (weeks === 1 ? "" : "s") + " ago"
    } else if (days > 0) {
        return days + " day" + (days === 1 ? "" : "s") + " ago"
    } else if (hours > 0) {
        return hours + " hour" + (hours === 1 ? "" : "s") + " ago"
    } else if (minutes > 0) {
        return minutes + " minute" + (minutes === 1 ? "" : "s") + " ago"
    } else if (seconds > 0) {
        return seconds + " second" + (seconds === 1 ? "" : "s") + " ago"
    }
    return undefined
}
