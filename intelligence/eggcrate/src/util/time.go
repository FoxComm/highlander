package util

import (
	"fmt"
	"time"
)

type Frequency interface {
	NextTime(time time.Time) time.Time
}

type seconds struct {
	seconds int64
}

type days struct {
	days   int
	months int
	years  int
}

func ParseFrequency(frequency string) (Frequency, error) {
	switch frequency {
	case "seconds", "second":
		return seconds{1}, nil
	case "minutes", "minute":
		return seconds{60}, nil
	case "hours", "hour":
		return seconds{3600}, nil
	case "days", "day":
		return days{1, 0, 0}, nil
	case "weeks", "week":
		return days{7, 0, 0}, nil
	case "months", "month":
		return days{0, 1, 0}, nil
	case "quarters", "quarter":
		return days{0, 3, 0}, nil
	case "years", "year":
		return days{0, 0, 1}, nil
	default:
		return nil, fmt.Errorf("invalid frequency %s", frequency)
	}
}

func (s seconds) NextTime(source time.Time) time.Time {
	return source.Add(time.Duration(s.seconds) * time.Second)
}

func (d days) NextTime(source time.Time) time.Time {
	return source.AddDate(d.months, d.years, d.days)
}

func SliceRangeToUnixTime(f Frequency, from time.Time, to time.Time) []int64 {
	slice := make([]int64, 0)

	var currentTime *time.Time = &from
	for currentTime.Before(to) {
		slice = append(slice, currentTime.Unix())
		next := f.NextTime(*currentTime)
		currentTime = &next
	}
	return append(slice, currentTime.Unix())
}
