#### Basic usage

```javascript
import Modal from 'components/core/modal';

<Modal
  isVisible={this.state.modalVisible}
  onCancel={this.handleClose}
/>
```

### Examples

#### ModalContainer

```javascript

import { ModalContainer } from 'components/core/modal';
```

```
const { Button } = require('../button/button.jsx');

const ModalContent = ModalContainer.withModal(() => <div>ModalWrapper Content</div>)

class ModalExample extends React.Component {
  constructor(props) {
    this.state = {
      visible: false,
    };
  }

  render() {
    return (
      <div>
        <ModalContainer.ModalContainer
          isVisible={this.state.visible}
          onCancel={() => this.setState({ visible: false })}
        >
          ModalContainer Content
        </ModalContainer.ModalContainer>

        <Button onClick={() => this.setState({ visible: true })}>
          Show modal
        </Button>
      </div>
    )
  }
}

<ModalExample />
```

#### withModal

```javascript

import { withModal } from 'components/core/modal';

// General component
const Content = (props: Props) => <div>Content</div>;

// Wrapaped in ModalContainer
export withModal(Content);

```

```
const { Button } = require('../button/button.jsx');

const ModalContent = ModalContainer.withModal(() => <div>ModalWrapper Content</div>)

class ModalExample extends React.Component {
  constructor(props) {
    this.state = {
      visible: false,
    };
  }

  render() {
    return (
      <div>
        <ModalContent
          isVisible={this.state.visible}
          onCancel={() => this.setState({ visible: false })}
        />

        <Button onClick={() => this.setState({ visible: true })}>
          Show modal
        </Button>
      </div>
    )
  }
}

<ModalExample />
```
