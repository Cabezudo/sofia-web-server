/*
 * Created on: 30/10/2018
 * Author:     Esteban Cabezudo
 */

const linkTo_1_00_00 = ({ onClick = null, id = null, element = null } = {}) => {
  const validateOptions = () => {
    if (id !== null) {
      element = Core.validateById(id);
    } else {
      if (element === null) {
        throw Error('You must define a property id or a property element.');
      }
    }
    if (onClick === null) {
      throw Error('You must define a url or function in a property onClick.');
    }
  };
  const createGUI = () => {
    element.classList.add('linkTo-1_00_00');
    element.onclick = () => {
      if (typeof onClick === 'function') {
        onClick(this);
      } else {
        document.location.href = onClick;
      }
    };
  };
  const assignTriggers = () => {
  };
  validateOptions();
  createGUI();
  assignTriggers();
}
;