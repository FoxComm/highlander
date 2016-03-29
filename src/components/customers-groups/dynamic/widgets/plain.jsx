//components
import { Input } from '../inputs/plain';
import { Label } from '../labels/plain';


export default function (type) {
  return {
    Input: Input(type),
    Label: Label(type)
  };
}
