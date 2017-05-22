#### Basic usage

```javascript
<Checkbox
  id='input-id'
  className={style.checkbox}
  docked='left'
>
  labelText
</Checkbox>
```

### Checkbox

```javascript
import { Checkbox } from 'components/core/checkbox'
```

```
<div>
  <div>
    <Checkbox.Checkbox checked>checked</Checkbox.Checkbox>
  </div>
  <div>
    <Checkbox.Checkbox>not checked</Checkbox.Checkbox>
  </div>
</div>
```

### PartialCheckbox

```javascript
import { PartialCheckbox } from 'components/core/checkbox'
```

```
<div className='demo'>
  <Checkbox.PartialCheckbox checked>checked</Checkbox.PartialCheckbox>
  <Checkbox.PartialCheckbox checked halfChecked>partially checked</Checkbox.PartialCheckbox>
  <Checkbox.PartialCheckbox>not checked</Checkbox.PartialCheckbox>
</div>
```


### SliderCheckbox

```javascript
import { SliderCheckbox } from 'components/core/checkbox'
```

```
<div className='demo'>
  <Checkbox.SliderCheckbox checked/>
  <Checkbox.SliderCheckbox/>
</div>
```
