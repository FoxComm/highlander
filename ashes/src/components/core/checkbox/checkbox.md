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
  <div>
      <Checkbox.Checkbox disabled>disabled</Checkbox.Checkbox>
  </div>
</div>
```

### PartialCheckbox

```javascript
import { PartialCheckbox } from 'components/core/checkbox'
```

```
<div className='demo'>
  <div>
    <Checkbox.PartialCheckbox checked>checked</Checkbox.PartialCheckbox>
  </div>
  <div>
    <Checkbox.PartialCheckbox checked halfChecked>partially checked</Checkbox.PartialCheckbox>
  </div>
  <div>
    <Checkbox.PartialCheckbox>not checked</Checkbox.PartialCheckbox>
  </div>
  <div>
      <Checkbox.PartialCheckbox disabled>disabled</Checkbox.PartialCheckbox>
  </div>
</div>
```

### BigCheckbox

```javascript
import { BigCheckbox } from 'components/core/checkbox'
```

```
<div className='demo'>
  <div>
    <Checkbox.BigCheckbox checked>checked</Checkbox.BigCheckbox>
  </div>
  <div>
    <Checkbox.BigCheckbox>not checked</Checkbox.BigCheckbox>
  </div>
  <div>
    <Checkbox.BigCheckbox disabled>disabled</Checkbox.BigCheckbox>
  </div>
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
