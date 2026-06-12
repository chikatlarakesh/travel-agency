import { ReactComponent as EmailIcon } from '../../assets/icons/mail.svg';
import { ReactComponent as PhoneIcon } from '../../assets/icons/phone.svg';
import { ReactComponent as MessengerIcon } from '../../assets/icons/messenger.svg';
import { ReactComponent as PersonIcon } from '../../assets/icons/Person.svg';

const AgentInfo = ({ agent }) => (
  <div className="flex flex-col gap-4">
    {/* Travel agent title */}
    <p
      className="font-nunito"
      style={{
        width: '100%',
        maxWidth: '296px',
        height: '24px',
        color: '#0B3857',
        fontSize: '14px',
        lineHeight: '24px',
        letterSpacing: '0%',
        fontWeight: 800,
      }}
    >
      Travel agent
    </p>

    {/* Details frame */}
    <div
      className="flex flex-col"
      style={{
        width: '100%',
        maxWidth: '296px',
        height: '96px',
        gap: '4px',
      }}
    >
      {/* Name row */}
      <div
        className="flex items-center"
        style={{
          width: '100%',
          maxWidth: '296px',
          height: '24px',
          gap: '8px',
        }}
      >
        <PersonIcon className="h-4 w-4 shrink-0 text-[#677883]" />
        <span
          className="font-nunito truncate"
          style={{
            width: '100%',
            maxWidth: '272px',
            height: '24px',
            color: '#0B3857',
            fontSize: '14px',
            lineHeight: '24px',
            letterSpacing: '0%',
            fontWeight: 400,
          }}
        >
          {agent.name}
        </span>
      </div>

      {/* Email row */}
      <div
        className="flex items-center"
        style={{
          width: '100%',
          maxWidth: '296px',
          height: '24px',
          gap: '8px',
        }}
      >
        <EmailIcon className="h-4 w-4 shrink-0 text-[#677883]" />
        <span
          className="font-nunito truncate cursor-pointer hover:underline"
          style={{
            width: '100%',
            maxWidth: '272px',
            height: '24px',
            color: '#0B3857',
            fontSize: '14px',
            lineHeight: '24px',
            letterSpacing: '0%',
            fontWeight: 400,
          }}
        >
          {agent.email}
        </span>
      </div>

      {/* Phone row */}
      <div
        className="flex items-center"
        style={{
          width: '100%',
          maxWidth: '296px',
          height: '24px',
          gap: '8px',
        }}
      >
        <PhoneIcon className="h-4 w-4 shrink-0 text-[#677883]" />
        <span
          className="font-nunito"
          style={{
            width: '100%',
            maxWidth: '272px',
            height: '24px',
            color: '#0B3857',
            fontSize: '14px',
            lineHeight: '24px',
            letterSpacing: '0%',
            fontWeight: 400,
          }}
        >
          {agent.phone}
        </span>
      </div>

      {/* Messenger row */}
      <div
        className="flex items-center"
        style={{
          width: '100%',
          maxWidth: '296px',
          height: '24px',
          gap: '8px',
        }}
      >
        <MessengerIcon className="h-4 w-4 shrink-0 text-[#677883]" />
        <span
          className="font-nunito cursor-pointer hover:underline"
          style={{
            width: '100%',
            maxWidth: '272px',
            height: '24px',
            color: '#0B3857',
            fontSize: '14px',
            lineHeight: '24px',
            letterSpacing: '0%',
            fontWeight: 400,
          }}
        >
          Messenger
        </span>
      </div>
    </div>
  </div>
);

export default AgentInfo;
