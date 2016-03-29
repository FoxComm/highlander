//components
import { Input, getDefault } from '../inputs/range';
import { Label } from '../labels/range';


export default function(Widget) {
  return {
    Input: Input(Widget),
    getDefault: getDefault(Widget),
    Label: Label(Widget),
  };
}
