import './ActionButtons.css';

const ActionButtons = ({ status, pendingCustomerApproval, onCancel, onEdit, onConfirm }) => {
  if (status === 'booked') {
    return (
      <div className="action-buttons">
        <button type="button" onClick={onCancel} className="action-buttons__btn action-buttons__btn--outline">Cancel</button>
        <button type="button" onClick={onEdit} className="action-buttons__btn action-buttons__btn--outline">Edit</button>
        {pendingCustomerApproval ? (
          <div className="action-buttons__confirm-wrapper">
            <button type="button" disabled className="action-buttons__btn action-buttons__btn--primary action-buttons__btn--disabled">Check and confirm</button>
            <div className="action-buttons__tooltip">Waiting for changes approval from customer</div>
          </div>
        ) : (
          <button type="button" onClick={onConfirm} className="action-buttons__btn action-buttons__btn--primary">Check and confirm</button>
        )}
      </div>
    );
  }

  if (status === 'confirmed') {
    return (
      <div className="action-buttons">
        <button type="button" onClick={onCancel} className="action-buttons__btn action-buttons__btn--outline">Cancel</button>
      </div>
    );
  }

  // started, finished, canceled → no actions
  return null;
};

export default ActionButtons;
