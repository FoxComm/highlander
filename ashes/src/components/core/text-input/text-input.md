##### Basic usage

```javascript
import { TextInput } from 'components/core/text-input'

<TextInput onChange={handleChange} value="some value" />
```

### States

```
<div className="demo">
  <div>
    <TextInput onChange={(e) => {input.value = e.target.value}} />
  </div>
  <div>
    <TextInput onChange={(e) => {input.value = e.target.value}} placeholder='i am placeholder' />
  </div>
  <div>
    <TextInput defaultValue="input with default value" />
  </div>
  <div>
    <TextInput value="disabled input" disabled />
  </div>

  <div>
    <TextInput value="input with error" error />
  </div>
</div>
```
