#### Basic usage

```javascript
import Counter from 'components/core/counter';

<Counter
  onChange={this.onChange}
  value={value}
  className={s.counter}
/>
```

### Simple Counter

```
class CounterExample extends React.Component {
  constructor() {
     this.state = {
        value1: 0,
        value2: 100,
     };
  }

  handleChange(name, value) {
    this.setState({ [name]: value });
  }

  render() {
    return  (
    <div className="demo-blocked">
      <Counter
        value={this.state.value1}
        onChange={v => this.handleChange('value1', v)}
      />
      <br />
      <Counter
        value={this.state.value2}
        onChange={v => this.handleChange('value2', v)}
        step={5}
        max={1000}
      />
    </div>
    )
  }
}

<CounterExample />
```
