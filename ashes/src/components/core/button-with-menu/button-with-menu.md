#### Description

Button component that represents a button with additional action in a dropdown menu.

#### Basic usage

```javascript
<ButtonWithMenu
  className={styles.button}
  icon="save"
  title="Save"
  menuPosition="right"
  items={[
      ['id1', 'Save and Exit'],
      ['id2', 'Save and Duplicate'],
  ]}
  onPrimaryClick={saveHandler}
  onSelect={menuSelectHandler}
  isLoading={saveState.inProgress}
  buttonDisabled={!saveState.finished}
  menuDisabled={!saveState.finished}
  animate
/>
```

### ButtonWithMenu

```javascript
import { ButtonWithMenu } from 'components/core/button-with-menu'
```

```
<ButtonWithMenu
  title="Save"
  menuPosition="right"q
  items={[
      ['id1', 'Save and Exit'],
      ['id2', 'Save and Duplicate'],
  ]}
  animate />
```

### States

```
<div className="demo">
  <SaveCancel />
  <SaveCancel isLoading />
  <SaveCancel saveDisabled />
  <SaveCancel cancelDisabled />
  <SaveCancel saveDisabled cancelDisabled />
</div>
```

### Save with menu

```javascript
import { SaveCancel } from 'components/core/save-cancel'
```

```
<SaveCancelq
  saveItems={[
    ['id1', 'Save and Close'],
    ['id2', 'Save and Duplicate'],
    ['id3', 'Save and Destroy!!!'],
  ]}
/>
```
