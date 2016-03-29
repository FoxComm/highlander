//components
import { Input } from '../inputs/range';
import { Label } from '../labels/range';


export default function(Widget) {
  return {
    Input: Input(Widget),
    Label: Label(Widget)
  };
}
