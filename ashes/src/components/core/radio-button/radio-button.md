#### Basic usage

```javascript
<RadioButton
  className={styles.radio}
  checked={someCondition}
  disabled={isDisabled}
>
  Content Text
</RadioButton>
```

### Simple RadioButton

```javascript
import { RadioButton } from 'components/core/radio-button'
```

```
<div>
  <RadioButton id="disabled" disabled>disabled</RadioButton>
  <RadioButton id="id1">clickable</RadioButton>
  <RadioButton id="id2" checked>checked</RadioButton>
</div>
```
