//components
import { Input, getDefault } from '../inputs/one-of';
import { Label } from '../labels/one-of';


export default function(Widget) {
  return {
    Input: Input(Widget),
    getDefault: getDefault(Widget),
    Label: Label(Widget),
  };
}
