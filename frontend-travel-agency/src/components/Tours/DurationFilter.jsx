import { useState } from 'react';
import { ReactComponent as ClockIcon } from '../../assets/icons/tourdetail.svg';
import MultiSelectShell from './MultiSelectShell';

const DURATION_OPTIONS = [
	{ value: 3, label: '1–3 days' },
	{ value: 5, label: '4–5 days' },
	{ value: 7, label: '6–7 days' },
	{ value: 10, label: '8+ days' },
];

const DurationFilter = ({ selected, onChange }) => {
	const [open, setOpen] = useState(false);

	const toggle = (val) =>
		onChange(
			selected.includes(val) ? selected.filter((v) => v !== val) : [...selected, val]
		);

	const summary = selected.length
		? DURATION_OPTIONS.filter((o) => selected.includes(o.value))
				.map((o) => o.label)
				.join(', ')
		: 'Any duration';

	return (
		<MultiSelectShell
			icon={<ClockIcon className="h-4 w-4" />}
			label="Duration"
			summary={summary}
			open={open}
			setOpen={setOpen}
		>
			{DURATION_OPTIONS.map((option) => (
				<label
					key={option.value}
					className={`flex cursor-pointer items-center justify-between rounded-xl px-3 py-2.5 transition hover:bg-primary-light ${
						selected.includes(option.value)
							? 'font-semibold text-primary'
							: 'text-brand-text'
					} font-nunito text-sm`}
				>
					{option.label}
					<input
						type="checkbox"
						checked={selected.includes(option.value)}
						onChange={() => toggle(option.value)}
						className="h-4 w-4 accent-primary rounded"
					/>
				</label>
			))}
		</MultiSelectShell>
	);
};

export default DurationFilter;
