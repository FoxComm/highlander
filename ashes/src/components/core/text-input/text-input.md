#### Basic usage

```javascript
<TextInput onChange={handleChange} value="some value"/>
```

### States

```javascript
import { TextInput } from 'components/core/text-input'
```

```
<div className="demo">
  <div>
    <TextInput onChange={(e) => {input.value = e.target.value}}/>
  </div>
  <div>
    <TextInput onChange={(e) => {input.value = e.target.value}} placeholder='i am placeholder'/>
  </div>
  <div>
    <TextInput value="input with value"/>
  </div>
  <div>
    <TextInput value="disabled input" disabled/>
  </div>
</div>
```
