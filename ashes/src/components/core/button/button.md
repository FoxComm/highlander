#### Basic usage

```javascript
<Button
  className={styles.button}
  icon="fetch"
  onClick={handler}
  isLoading={fetchState.inProgress}
  disabled={!fetchState.finished}
>
  Fetch Items
</Button>
```

### Generic Button

```javascript
import { Button } from 'components/core/button'
```

```
<Button.Button>Push Me</Button.Button>
```

### States

```
<div className="demo">
  <Button.Button>Ready</Button.Button>
  <Button.Button isLoading>Loading...</Button.Button>
  <Button.Button disabled>Disabled</Button.Button>
  <Button.Button fullWidth>Stretched button</Button.Button>
  <Button.Button icon="add">Add</Button.Button>
  <Button.Button icon="add" />
  <Button.Button small>Small</Button.Button>
  <Button.Button icon="add" small>Add</Button.Button>
  <Button.Button icon="add" small />
  <Button.Button>Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum.</Button.Button>
</div>
```

#### Primary Button:

```javascript
import { PrimaryButton } from 'components/core/button'
```

```
<div className="demo">
  <Button.PrimaryButton>Push Me</Button.PrimaryButton>
  <Button.PrimaryButton isLoading>Loading...</Button.PrimaryButton>
  <Button.PrimaryButton disabled>Disabled</Button.PrimaryButton>
  <Button.PrimaryButton fullWidth icon="google">Push Me</Button.PrimaryButton>
</div>
```

#### Left Button:

```javascript
import { LeftButton } from 'components/core/button'
```

```
<div className="demo">
  <Button.LeftButton>Left</Button.LeftButton>
  <Button.LeftButton isLoading>Left</Button.LeftButton>
  <Button.LeftButton disabled>Left</Button.LeftButton>
  <Button.LeftButton />
</div>
```

#### Right Button:

```javascript
import { RightButton } from 'components/core/button'
```

```
<div className="demo">
  <Button.RightButton>Right</Button.RightButton>
  <Button.RightButton isLoading>Right</Button.RightButton>
  <Button.RightButton disabled>Right</Button.RightButton>
  <Button.RightButton />
</div>
```

#### Decrement Button:

```javascript
import { DecrementButton } from 'components/core/button'
```

```
<div className="demo">
  <Button.DecrementButton>Less</Button.DecrementButton>
  <Button.DecrementButton isLoading>Less</Button.DecrementButton>
  <Button.DecrementButton disabled>Less</Button.DecrementButton>
  <Button.DecrementButton />
</div>
```

#### Increment Button:

```javascript
import { IncrementButton } from 'components/core/button'
```

```
<div className="demo">
  <Button.IncrementButton>More</Button.IncrementButton>
  <Button.IncrementButton isLoading>More</Button.IncrementButton>
  <Button.IncrementButton disabled>More</Button.IncrementButton>
  <Button.IncrementButton />
</div>
```

#### Add Button:

```javascript
import { AddButton } from 'components/core/button'
```

```
<div className="demo">
  <Button.AddButton>Add</Button.AddButton>
  <Button.AddButton isLoading>Add</Button.AddButton>
  <Button.AddButton disabled>Add</Button.AddButton>
  <Button.AddButton />
</div>
```

#### Edit Button:

```javascript
import { EditButton } from 'components/core/button'
```

```
<div className="demo">
  <Button.EditButton>Edit</Button.EditButton>
  <Button.EditButton isLoading>Edit</Button.EditButton>
  <Button.EditButton disabled>Edit</Button.EditButton>
  <Button.EditButton />
</div>
```

#### Delete Button:

```javascript
import { DeleteButton } from 'components/core/button'
```

```
<div className="demo">
  <Button.DeleteButton>Delete</Button.DeleteButton>
  <Button.DeleteButton isLoading>Delete</Button.DeleteButton>
  <Button.DeleteButton disabled>Delete</Button.DeleteButton>
  <Button.DeleteButton />
</div>
```

#### Social Button:

```javascript
import { SocialButton } from 'components/core/button'
```

```
<div className="demo">
  <Button.SocialButton type="google">Social</Button.SocialButton>
  <Button.SocialButton type="google" isLoading>Social</Button.SocialButton>
  <Button.SocialButton type="google" disabled>Social</Button.SocialButton>
  <Button.SocialButton type="google" />
</div>
```
