#### Basic usage

```javascript
import Modal from 'components/core/modal';

<Modal
  isVisible={this.state.modalVisible}
  onCancel={this.handleClose}
  title="Base Modal"
/>
```

### Examples

```javascript

import Modal from 'components/core/modal';
```

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
          onCancel={() => this.setState({ visible: false })}
          title="Modal Example"
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
