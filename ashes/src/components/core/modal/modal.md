##### Basic usage

```javascript
import Modal from 'components/core/modal';

<Modal
  isVisible={this.state.modalVisible}
  onClose={this.setState({ modalVisible: false })}
  title="Base Modal"
  footer={<Button>Close</Button>}
>
  Modal Content
</Modal>
```

### Examples

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
        <Modal
          isVisible={this.state.visible}
          onClose={() => this.setState({ visible: false })}
          title="Modal Example"
          footer="Footer Content"
        >
          ModalContainer Content
        </Modal>

        <Button onClick={() => this.setState({ visible: true })}>
          Show modal
        </Button>
      </div>
    )
  }
}

<ModalExample />
```
