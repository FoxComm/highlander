##### Basic usage

```javascript
<TextMask mask={[/\d/]} />
```

### States

```javascript
import { TextMask } from 'components/text-mask'
```

```
const phoneArrMask = ['(', /[1-9]/, /\d/, /\d/, ')', ' ', /\d/, /\d/, /\d/, '-', /\d/, /\d/, /\d/, /\d/];
const strMask = '(999) 999 99 99';

<div className="demo">
  <div className="demo-inline">
    <TextMask.TextMask mask={false} />
    <TextMask.TextMask mask={false} value="1234567890" />
    <TextMask.TextMask mask={phoneArrMask} />
    <TextMask.TextMask mask={phoneArrMask} value="123456789" />
  </div>

  <div className="demo-inline">
    <TextMask.TextMask mask={phoneArrMask} showMask />
    <TextMask.TextMask mask="(999) 999 99 99" />
    <TextMask.TextMask mask="aaa-aa" value="abcef" />
    <TextMask.TextMask mask="http//www.www.www" value="http://www.fox.com" />
  </div>

  <div className="demo-inline">
    <TextMask.TextMask mask="9999 9999 9999 9999" />
  </div>
</div>
```
