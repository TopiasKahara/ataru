
create table jobs (
  id              bigserial primary key,
  job_type        varchar(255)                  -- e.g. "ataru.background-job.example-job"
);


-- Each iteration of a step execution creates a new row in this table.
create table job_iterations (
  id               bigserial primary key,
  job_id           bigint references jobs(id),
  step             varchar(255),                   -- step type, e.g. "initial"
  transition       varchar(100),                   -- the transition which resulted in this iteration and step, e.g. "to-next" or "retry"
  state            jsonb not null,                 -- Any job-specific state at a point in time
  next_activation  timestamp with time zone,
  retry_count      integer default 0,              -- How many retries have been made so far
  executed boolean default FALSE,                  -- Has the step been executed already
  execution_time   timestamp with time zone,
  final    boolean default FALSE,
  caused_by_error  text                            -- If the previous iteration of current step resulted in error (usually exception), details are here. Text because stacktraces can be large.
);
