#### Basic usage

```javascript
import ConfirmationModal from 'components/core/confirmation-modal';

<ConfirmationModal
  title="Are you sure?"
  isVisible={this.state.modalVisible}
  onCancel={this.setState({ modalVisible: false })}
  onConfirm={this.setState({ modalVisible: false })}
>
  Really sure?
</ConfirmationModal>
```

### Examples

#### Default
```
const { Button } = require('../button/button.jsx');

class ModalExample extends React.Component {
  constructor(props) {
    this.state = {
      visible: false,
    };
  }

  render() {
    return (
      <div>
        <ConfirmationModal
          title="Are you sure?"
          label="Really sure?"
          isVisible={this.state.visible}
          onCancel={() => this.setState({ visible: false })}
          onConfirm={() => this.setState({ visible: false })}
        />

        <Button onClick={() => this.setState({ visible: true })}>
          Delete
        </Button>
      </div>
    )
  }
}

<ModalExample />
```

#### Custom save/cancel titles
```
const { Button } = require('../button/button.jsx');

class ModalExample extends React.Component {
  constructor(props) {
    this.state = {
      visible: false,
    };
  }

  render() {
    return (
      <div>
        <ConfirmationModal
          title="Are you sure?"
          isVisible={this.state.visible}
          onCancel={() => this.setState({ visible: false })}
          onConfirm={() => this.setState({ visible: false })}
          cancelLabel="Noooooooooooooöóoooo!!!!!"
          confirmLabel="😀"
        >
          Really sure?
        </ConfirmationModal>

        <Button onClick={() => this.setState({ visible: true })}>
          Delete
        </Button>
      </div>
    )
  }
}

<ModalExample />
```
