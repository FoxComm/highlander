#### Basic usage

```javascript
import Currency from 'components/utils/currency';

<Currency value={200} isTransaction />
```

### States
```
<div className="demo-inline">
  <Currency value={100}  />
  <Currency value={200} isTransaction />
  <Currency value={-200} isTransaction />
  <Currency value={0} isTransaction />
</div>

```

### Example
```
class CurrencyExample extends React.Component {
  constructor() {
    this.state = {
      value: 0,
      isTransaction: false
    };
  }

  checkboxChange() {
    this.setState({ isTransaction: !this.state.isTransaction });
  }

  inputChange(value) {
    this.setState({ value });
  }

  render() {
    return (
      <div className="demo">
        <div>
          <Checkbox.Checkbox
            id='isTransaction'
            label='isTransaction'
            onChange={this.checkboxChange.bind(this)} />
        </div>
        <div>
          <TextInput
            value={this.state.value}
            onChange={this.inputChange.bind(this)}
            placeholder="Enter value" />
        </div>
        <div>
          Result:
          <Currency
            value={this.state.value}
            isTransaction={this.state.isTransaction} />
        </div>
      </div>
    );
  }
}

<CurrencyExample />
```
