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
  <Checkbox.Checkbox id="checked" label="checked" defaultChecked /><br />
  <Checkbox.Checkbox id="not-checked" label="not checked" /><br />
  <Checkbox.Checkbox id="disabled" label="disabled" disabled /><br />
  
  <h6>Checkbox in table cell</h6>
  <table style={{ width: '60px' }}>
    <tr>
      <td style={{ position: 'relative', padding: '20px', border: '1px solid #ddd' }}>
        <Checkbox.Checkbox id="in-cell" inCell />
      </td>
    </tr>
  </table>
</div>
```

### PartialCheckbox

```javascript
import { PartialCheckbox } from 'components/core/checkbox'
```

```
<div className='demo'>
  <Checkbox.PartialCheckbox id="pc-checked" label="checked" defaultChecked /><br />
  <Checkbox.PartialCheckbox id="pc-partially" label="partially checked" defaultChecked halfChecked /><br />
  <Checkbox.PartialCheckbox id="pc-not-checked" label="not checked" /><br />
  <Checkbox.PartialCheckbox id="pc-disabled" label="disabled" disabled /><br />
</div>
```

### BigCheckbox

```javascript
import { BigCheckbox } from 'components/core/checkbox'
```

```
<div className='demo'>
  <Checkbox.BigCheckbox id="bc-checked" label="checked" defaultChecked /><br />
  <Checkbox.BigCheckbox id="bc-not-checked" label="not checked" /><br />
  <Checkbox.BigCheckbox id="bc-disabled" label="disabled" disabled /><br />
</div>
```


### SliderCheckbox

```javascript
import { SliderCheckbox } from 'components/core/checkbox'
```

```
<div className='demo'>
  <Checkbox.SliderCheckbox id="sc-checked" checked/><br />
  <Checkbox.SliderCheckbox id="sc-not-checked"/><br />
</div>
```
