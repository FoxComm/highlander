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
    <Checkbox.Checkbox halfChecked checked>half checked</Checkbox.Checkbox>
  </div>
  <div>
    <Checkbox.Checkbox/>
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
