##### Basic usage

```javascript
import Countdown from 'components/core/countdown';

<Countdown
  endDate={moment().add(5, 'm').utc().format()}
/>
```

### States

```
const moment = require('moment');

<div className="demo">
  <div className="demo-inline">
    <Countdown
      endDate={moment().add(5, 'm').utc().format()}
    />

    <Countdown
      endDate={moment().add(5, 'm').utc().format()}
      endingThreshold={moment.duration(10, 'm')}
    />

    <Countdown
      endDate={moment().add(5, 'm').utc().format()}
      endingThreshold={moment.duration(1, 'm')}
      frozen
    />

    <Countdown
      endDate={moment().add(5, 'm').utc().format()}
      format="HH-mm-ss-SSS"
    />

    <Countdown
      endDate={moment().add(5, 'm').utc().format()}
      format="HH-mm-ss-SSS"
      tickInterval={1}
    />
  </div>
</div>
```
