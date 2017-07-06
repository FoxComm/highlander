##### Basic usage

```javascript
import ModalContainer from 'components/core/modal-container';

<ModalContainer
  isVisible={this.state.modalVisible}
  onClose={this.setState({ modalVisible: false })}
  className={s.customModalClassName}
>
  Modal Content
</ModalContainer>
```

### Examples

#### ModalContainer

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
        <ModalContainer
          isVisible={this.state.visible}
          onClose={() => this.setState({ visible: false })}
        >
          ModalContainer Content
        </ModalContainer>

        <Button onClick={() => this.setState({ visible: true })}>
          Show modal
        </Button>
      </div>
    )
  }
}

<ModalExample />
```
