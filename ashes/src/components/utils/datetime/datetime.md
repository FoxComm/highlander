#### Basic usage

```javascript
<DateTime value={1497636772107} />
```

### States

```javascript
import { Moment } from 'components/utils/datetime'
```

```
<div className="demo" >
  <Moment.Moment value={1497636772107} /><br />
  <Moment.Moment value={1497636772107} format="L LT" /><br />
  <Moment.Moment value={1497636772107} format="L" /><br />
  <Moment.Moment value={1497636772107} format="l" /><br />
  <Moment.Moment value={1497636772107} format="LT" /><br />
  <Moment.Moment format="LT" />
</div>
```

```javascript
import { DateTime } from 'components/utils/datetime'
```

```
<div className="demo" >
  <Moment.DateTime value={1497636772107} />
</div>
```

```javascript
import { Date } from 'components/utils/datetime'
```

```
<div className="demo" >
  <Moment.Date value={1497636772107} />
</div>
```

```javascript
import { Time } from 'components/utils/datetime'
```

```
<div className="demo" >
  <Moment.Time value={1497636772107} />
</div>
```
