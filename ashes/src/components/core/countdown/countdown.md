#### Basic usage

```javascript
import { Countdown } from 'components/core/countdown';

<Countdown
  endDate={moment().add(5, 'm').utc().format()}
/>
```

### States

#### Default state

```
const moment = require('moment');

<Countdown.Countdown
  endDate={moment().add(5, 'm').utc().format()}
/>
```

#### Ending

```
const moment = require('moment');

<Countdown.Countdown
  endDate={moment().add(5, 'm').utc().format()}
  endingThreshold={moment.duration(10, 'm')}
/>
```

#### Frozen

```
const moment = require('moment');

<Countdown.Countdown
  endDate={moment().add(5, 'm').utc().format()}
  endingThreshold={moment.duration(1, 'm')}
  frozen
/>
```

#### Other format

```
const moment = require('moment');

<Countdown.Countdown
  endDate={moment().add(5, 'm').utc().format()}
  format="HH-mm-ss-SSS"
/>
```

#### Custom tick interval

```
const moment = require('moment');

<Countdown.Countdown
  endDate={moment().add(5, 'm').utc().format()}
  format="HH-mm-ss-SSS"
  tickInterval={1}
/>
```
