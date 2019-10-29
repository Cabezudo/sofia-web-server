/*
 * Created on: 30/10/2018
 * Author:     Esteban Cabezudo
 */

/* global Core */

const inputEMailValidator = ({ element = null, onValid = null, onNotValid = null, onKeyPress = null } = {}) => {
  let verificationTimer;
  let requestId = 0;

  const validateOptions = () => {
    if (element === null) {
      throw Error('You must define an element to apply the validator.');
    }
  };
  const createGUI = () => {
    element.className = 'inputEMailValidator';
    if (element.value && element.value.length > 0) {
      sendValidationRequest(element);
    }
  };
  const assignTriggers = () => {
    element.addEventListener('response', event => {
      const data = event.detail;

      const element = event.srcElement;
      if (requestId === data.requestId) {
        Core.cleanMessagesContainer();
        const data = event.detail;
        data.elementId = element.id;
        Core.addMessage(data);
        if (data.status === 'ERROR') {
          element.classList.add('error');
        }
        if (data.status === 'OK') {
          element.classList.remove('error');
          if (Core.isFunction(onValid)) {
            onValid();
          }
        } else {
          if (Core.isFunction(onNotValid)) {
            onNotValid();
          }
        }
      }
    });
    element.addEventListener("keypress", event => {
      if (Core.isFunction(onKeyPress)) {
        onKeyPress(event);
      }
    });
    element.addEventListener("keyup", event => {
      if (Core.isModifierKey(event) || Core.isNavigationKey(event)) {
        return;
      }
      if (verificationTimer) {
        clearTimeout(verificationTimer);
      }
      verificationTimer = setTimeout(sendValidationRequest(element), Core.EVENT_TIME_DELAY);
    });
  };
  const sendValidationRequest = element => {
    const response = Core.sendGet(`/api/v1/mails/${element.value}/validate`, element);
    requestId = response.requestId;
  };

  validateOptions();
  createGUI();
  assignTriggers();
}
;