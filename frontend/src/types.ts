// Shapes returned by the Spring Boot API (see backend ApiDtos).

export type Role = 'Administrator' | 'Instructor' | 'Member';

export interface User {
  id: string;
  name: string;
  role: Role;
}

export type EquipmentStatus = 'OPERATIONAL' | 'FAULTY' | 'UNDER_MAINTENANCE';

export interface Equipment {
  id: string;
  name: string;
  type: string;
  location: string;
  status: EquipmentStatus;
  active: boolean;
  totalUsageHours: number;
  hoursSinceMaintenance: number;
  maintenanceThresholdHours: number;
  needsMaintenance: boolean;
}

export interface Session {
  id: string;
  title: string;
  instructorId: string;
  instructorName: string;
  studio: string;
  start: string;
  end: string;
  capacity: number;
  booked: number;
  availableSpots: number;
  equipmentIds: string[];
}

export type Urgency = 'LOW' | 'MEDIUM' | 'HIGH';
export type RequestStatus = 'PENDING' | 'ASSIGNED' | 'COMPLETED';

export interface MaintenanceRequest {
  id: string;
  equipmentId: string;
  description: string;
  urgency: Urgency;
  status: RequestStatus;
  reportedBy: string;
  assignedTo: string | null;
  progressNotes: string[];
}

/** A message shown in the banner: success or an API error in plain words. */
export interface Message {
  kind: 'ok' | 'error';
  text: string;
}

export type Report = (message: Message | null) => void;
