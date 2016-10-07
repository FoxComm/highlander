
declare class CheckResult {
  password: string;
  guesses: number;
  guesses_log10: number;
  sequence: Array<any>;
  calc_time: number;
  crack_times_seconds: {
    online_throttling_100_per_hour: number;
    online_no_throttling_10_per_second: number;
    offline_slow_hashing_1e4_per_second: number;
    offline_fast_hashing_1e10_per_second: number;
  };
  crack_times_display: {
    online_throttling_100_per_hour: number;
    online_no_throttling_10_per_second: number;
    offline_slow_hashing_1e4_per_second: number;
    offline_fast_hashing_1e10_per_second: number;
  };
  score: number;
  feedback: {
    warning: string;
    suggestions: Array<string>;
  }
}

declare function zxcvbn(password: string): CheckResult;
