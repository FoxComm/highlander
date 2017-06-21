#### Basic usage

```javascript
import RadioButton from 'components/core/radio-button';

<RadioButton
  id="button"
  className={s.radio}
  label="Option #1"
  checked={this.state.radioChecked}
  disabled={this.state.radioDisabled}
/>
```

### Simple RadioButton

```
<div>
  <RadioButton label="disabled" id="disabled" disabled />
  <RadioButton label="clickable #1" name="value" id="id1" />
  <RadioButton label="clickable #2" name="value" id="id2" />
</div>
```
