#### Basic usage

```javascript
<SwatchInput onChange={handleChange} value="ff0000" />
```

### Simple SwatchInput

```javascript
import { SwatchInput } from 'components/core/swatch-input'
```

```
const red = 'ff0000';
const black = '000000';
const white = 'ffffff';

<div className="demo">
  <SwatchInput value={ red } />
  <SwatchInput value={ black } />
  <SwatchInput value={ white } />
</div>
```
